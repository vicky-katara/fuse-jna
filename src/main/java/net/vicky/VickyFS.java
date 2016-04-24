package net.vicky;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.fusejna.ErrorCodes;

/*
 * struct VPoint { char* name; int isDirectory; char* contents; int current_content_capacity; struct Queue *subQueue; struct
 * VPoint *parent; struct OpenVPoint *openContainerForThis;
 */
public class VickyFS
{
	public static boolean DEBUG_MODE_ON = false;
	public static boolean DEBUG_FILE_SIZE_MODE_ON = true;

	static Byte[] getLarge(final byte[] arr)
	{
		final Byte[] retArr = new Byte[arr.length];
		for (int index = 0; index < retArr.length; index++) {
			retArr[index] = arr[index];
		}
		return retArr;
	}

	static byte[] getSmall(final Byte[] arr)
	{
		final byte[] retArr = new byte[arr.length];
		for (int index = 0; index < retArr.length; index++) {
			retArr[index] = arr[index];
		}
		return retArr;
	}

	public static void main(final String[] args)
	{
		final VickyFS firstVFS = new VickyFS(50);
		firstVFS.create_point("dirA", VPoint.IS_DIRECTORY);
		firstVFS.create_point("dirA/dirA1", VPoint.IS_DIRECTORY);
		firstVFS.create_point("dirA/dirA1/dirA11", VPoint.IS_DIRECTORY);
		// if (VickyFS.DEBUG_MODE_ON) System.out.println(firstVFS);
		firstVFS.create_point("dirA/dirA1/dirA11/fileA11a", VPoint.IS_FILE);
		firstVFS.create_point("dirA/dirA1/dirA11/fileA11b", VPoint.IS_FILE);
		final int A11aFD = firstVFS.open_file("dirA/dirA1/dirA11/fileA11a");
		final byte[] bArr = new byte[] { 8, 80, 16, 89, 19, 42, 11, 56, 74, 19, 61, 34, 84, 100, 21, 71, 63, 9, 85, 61, 34, 82,
				38, 86, 21, 82, 27, 86, 71, 9, 28, 43, 23, 55, 20, 36, 11, 57, 4, 43, 85, 68, 83, 32, 7, 80, 68, 44, 13, 46, 84,
				90, 17, 83, 19, 10, 52, 5, 79, 23, 19, 41, 65, 59 };
		firstVFS.vwrite(A11aFD, bArr.length, 0, ByteBuffer.wrap(bArr));
		// reading
		final ByteBuffer readBuf = ByteBuffer.wrap(new byte[100]);
		firstVFS.vread(A11aFD, readBuf.remaining() - 1, 0, readBuf);
		final byte[] readArr = readBuf.array();
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("fileA11a contains: " + Arrays.toString(readArr));
		}
		firstVFS.change_dir("dirA");
		firstVFS.change_dir("dirA1/dirA11");
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(firstVFS.currentDir);
		}
		firstVFS.remove_point("fileA11a");
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(firstVFS.currentDir);
		}
		firstVFS.change_dir("../..");
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(firstVFS.currentDir);
		}
		firstVFS.remove_point("dirA1/dirA11");
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(firstVFS.currentDir);
		}
		firstVFS.change_dir("dirA1/dirA11");
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(firstVFS.currentDir);
		}
	}

	VPoint rootDir;
	VPoint currentDir;
	HashMap<Integer, VPoint> openFileMap;
	int lastAllocatedFD;
	// String current_path_str;
	long size, remaining_space;

	VickyFS(final long size)
	{
		lastAllocatedFD = 100;
		rootDir = new VPoint("/", VPoint.IS_DIRECTORY, null);
		currentDir = rootDir;
		openFileMap = new HashMap<Integer, VPoint>();
		// current_path_str = "/";
		this.size = size;
		remaining_space = size;
	}

	boolean change_dir(final String path)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.out.println("  >>> change_dir called with path:" + path);
			}
		}
		if (path.equals("/") || path.equals("")) {
			currentDir = rootDir;
			return true;
		}
		final String remainingPath = resolvePath(path);
		if (traverseToNewDirInCurrentDir(remainingPath) == true) {
			return true;
		}
		else {
			if (VickyFS.DEBUG_MODE_ON) {
				System.out.println(rootDir.name);
			}
			return false;
		}
	}

	boolean close_file_point(final int fd)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> close_file_point called with fd:" + fd);
		}
		if (openFileMap.containsKey(fd)) {
			openFileMap.remove(fd);
			return true;
		}
		else {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println("No Such FD Mapped to an Open File");
			}
			return false;
		}
	}

	int create_point(final String path, final boolean type)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> create_point called with path:" + path);
		}
		// save currentDir
		final VPoint oldCurrent = currentDir;
		final String newPointName = resolvePath(path);
		final VPoint newPoint = new VPoint(newPointName, type, currentDir);
		if (generateSpaceFor(newPoint) == false) {
			return -ErrorCodes.ENOMEM();
		}
		final int success = currentDir.addChildToDir(newPoint);
		// load back curretnDir
		currentDir = oldCurrent;
		return success;
	}

	boolean createNewPointUnderCurrentDir(final String newPointName, final boolean type)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> createNewPointUnderCurrentDir called with path:" + newPointName);
		}
		if (newPointName == null || newPointName.equals("") || newPointName.length() == 0) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println("A point cannot be created with <blank> name");
			}
			return false;
		}
		else if (currentDir.searchForChildPoint(newPointName) == true) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println(currentDir.name + " already  contains " + newPointName + " .");
			}
			return false;
		}
		else {
			final VPoint newVPoint = new VPoint(newPointName, type, currentDir);
			if (generateSpaceFor(newVPoint) == true) {
				if (VickyFS.DEBUG_MODE_ON) {
					System.out.println(newVPoint.name + " added under " + currentDir.name);
				}
				currentDir.addChildToDir(newVPoint);
				return true;
			}
			else {
				if (VickyFS.DEBUG_MODE_ON) {
					System.err.println("Not enough space!!");
				}
				return false;
			}
		}
	}

	// int createPoint(final String path, final boolean type)
	// {
	// if (VickyFS.DEBUG_MODE_ON) System.out.println(" >>> createPoint called with path:" + path);
	// // save currentDir
	// final VPoint oldCurrent = currentDir;
	// final String newPointName = resolvePath(path);
	// final VPoint newPoint = new VPoint(newPointName, type, currentDir);
	// if (generateSpaceFor(newPoint) == false) {
	// return -ErrorCodes.ENOMEM();
	// }
	// final int success = currentDir.addChildToDir(newPoint);
	// // load back curretnDir
	// currentDir = oldCurrent;
	// return success;
	// }
	boolean generateSpaceFor(final VPoint newPoint)
	{
		if (VickyFS.DEBUG_FILE_SIZE_MODE_ON) {
			System.out.println("  >>> generateSpaceFor called with path:" + newPoint);
		}
		int newSpaceNeeded = newPoint.name.length(); // char is 2 bytes
		if (newPoint.isFile()) {
			newSpaceNeeded += newPoint.contents.size();
		}
		if (newSpaceNeeded < remaining_space) {
			remaining_space -= newSpaceNeeded;
			if (VickyFS.DEBUG_FILE_SIZE_MODE_ON) {
				System.out.println("New space added: " + newSpaceNeeded + " Remaining: " + remaining_space + " Total: " + size);
			}
			return true;
		}
		else {
			if (VickyFS.DEBUG_FILE_SIZE_MODE_ON) {
				System.out.println("New space added: " + newSpaceNeeded + " Remaining: " + remaining_space + " Total: " + size);
			}
			return false;
		}
	}

	Integer getFDForOpenFileIfExits(final String filePointName)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> getFDForOpenFileIfExits called with path:" + filePointName);
		}
		if (openFileMap.containsValue(new VPoint(filePointName))) {
			for (final Map.Entry<Integer, VPoint> entry : openFileMap.entrySet()) {
				final VPoint existingPoint = entry.getValue();
				if (existingPoint.name.equals(filePointName) && existingPoint.parentPoint == currentDir) {
					return entry.getKey();
				}
			}
		}
		return null;
	}

	boolean getSpaceOf(final int newDataSize)
	{
		final int newSpaceNeeded = newDataSize; // char is 2 bytes
		if (newSpaceNeeded < remaining_space) {
			remaining_space -= newSpaceNeeded;
			if (VickyFS.DEBUG_FILE_SIZE_MODE_ON) {
				System.out.println("New space added: " + newSpaceNeeded + " Remaining: " + remaining_space + " Total: " + size);
			}
			return true;
		}
		else {
			if (VickyFS.DEBUG_FILE_SIZE_MODE_ON) {
				System.err.println(
						"Tried to add New space: " + newSpaceNeeded + " Remaining: " + remaining_space + " Total: " + size);
			}
			return false;
		}
	}

	int open_file(final String path)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> open_file called with path:" + path);
		}
		// save currentDir
		final VPoint oldCurrent = currentDir;
		final String newPointName = resolvePath(path);
		final int retVal = openFileInCurrentDir(newPointName);
		// load back curretnDir
		currentDir = oldCurrent;
		return retVal;
	}

	int openFileInCurrentDir(final String filePointName)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> openFileInCurrentDir called with path:" + filePointName);
		}
		final Integer existingFD = getFDForOpenFileIfExits(filePointName);
		if (existingFD != null) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println(filePointName + " was already open.");
			}
			return existingFD;
		}
		else if (filePointName == null || filePointName.equals("") || filePointName.length() == 0) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println("A point cannot exist with <blank> name");
			}
			return -ErrorCodes.ENAMETOOLONG();
		}
		else if (currentDir.searchForChildPoint(filePointName) == true) {
			final VPoint toBeOpened = currentDir.returnSubPoint(filePointName);
			if (toBeOpened.isDirectory()) {
				if (VickyFS.DEBUG_MODE_ON) {
					System.err.println(toBeOpened.name + " is not a file. Cannot open...");
				}
				return -ErrorCodes.EISDIR();
			}
			else {
				openFileMap.put(++lastAllocatedFD, toBeOpened);
				return lastAllocatedFD;
			}
		}
		if (VickyFS.DEBUG_MODE_ON) {
			System.err.println("Should not be here. openFileInCurrentDir.");
		}
		return -ErrorCodes.ENOENT();
	}

	void recoverSpaceFor(final VPoint removalPoint)
	{
		if (VickyFS.DEBUG_FILE_SIZE_MODE_ON) {
			System.out.println("  >>> recoverSpaceFor called with path:" + removalPoint);
		}
		int spaceRecovered = removalPoint.name.length(); // char is 2 bytes
		if (removalPoint.isFile()) {
			spaceRecovered += removalPoint.contents.size();
		}
		remaining_space += spaceRecovered;
		if (VickyFS.DEBUG_FILE_SIZE_MODE_ON) {
			System.out.println("New space added: " + spaceRecovered + " Remaining: " + remaining_space + " Total: " + size);
		}
	}

	boolean recoverSpaceOf(final int oldDataSize)
	{
		final int newSpaceNeeded = oldDataSize; // char is 2 bytes
		remaining_space += newSpaceNeeded;
		if (VickyFS.DEBUG_FILE_SIZE_MODE_ON) {
			System.out.println("New space added: " + newSpaceNeeded + " Remaining: " + remaining_space + " Total: " + size);
		}
		return true;
	}

	boolean remove_point(final String path)
	{
		if (VickyFS.DEBUG_FILE_SIZE_MODE_ON) {
			System.out.println("  >>> remove_point called with path:" + path);
		}
		boolean success;
		// save currentDir
		final VPoint oldCurrent = currentDir;
		final String newPointName = resolvePath(path);
		final VPoint toBeRemoved = currentDir.returnSubPoint(newPointName);
		if (toBeRemoved == null) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println("Could not find " + newPointName + " inside " + currentDir);
			}
			success = false;
		}
		else {
			success = currentDir.removeChildFromDir(toBeRemoved);
			if (toBeRemoved.isFile()) {
				final Integer openFD = getFDForOpenFileIfExits(toBeRemoved.name);
				final VPoint openPoint = openFileMap.get(openFD);
				if (openPoint != null && openPoint.parentPoint == currentDir) {
					close_file_point(openFD);
				}
			}
		}
		// load back curretnDir
		currentDir = oldCurrent;
		return success;
	}

	/*
	 * boolean removeOnePathLevel() { final int last_index_of_seperator = current_path_str.lastIndexOf('/'); if
	 * (last_index_of_seperator != -1) { current_path_str = current_path_str.substring(0, last_index_of_seperator); return true;
	 * } else { return false; } }
	 */
	boolean removePointUnderCurrentDir(final String exisingPointName)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> removePointUnderCurrentDir called with path:" + exisingPointName);
		}
		if (exisingPointName == null || exisingPointName.equals("") || exisingPointName.length() == 0) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println("A point cannot exist with <blank> name");
			}
			return false;
		}
		else if (currentDir.searchForChildPoint(exisingPointName) == false) {
			// if (VickyFS.DEBUG_MODE_ON) System.err.println(currentDir.name + " does not contains " + exisingPointName + " .");
			return false;
		}
		else {
			final VPoint toBeRemoved = currentDir.returnSubPoint(exisingPointName);
			recoverSpaceFor(toBeRemoved);
			if (VickyFS.DEBUG_MODE_ON) {
				System.out.println(toBeRemoved.name + " removed from under " + currentDir.name);
			}
			currentDir.removeChildFromDir(toBeRemoved);
			return true;
		}
	}

	String resolvePath(final String originalPath)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> resolvePath called with path:" + originalPath);
		}
		currentDir = rootDir;
		int index = 1;
		final String[] dirs = originalPath.split("/");
		if (dirs.length == 0) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.out.println("Nothing to resolve. Returning " + originalPath);
			}
			return originalPath;
		}
		while (index < dirs.length - 1) {
			final String nextDir = dirs[index++];
			traverseToNewDirInCurrentDir(nextDir);
			if (VickyFS.DEBUG_MODE_ON) {
				System.out.print("/ >" + nextDir);
			}
		}
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("\nResolved");
		}
		return dirs[dirs.length - 1];
	}

	VPoint return_point(final String path)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> return_point called with path:" + path);
		}
		// save currentDir
		if (currentDir == rootDir && path.equals("/")) {
			return rootDir;
		}
		final VPoint oldCurrent = currentDir;
		final String newPointName = resolvePath(path);
		final VPoint return_point;
		if (newPointName.equals(currentDir.name)) {
			return_point = currentDir;
		}
		else {
			return_point = returnPointInCurrentDir(newPointName);
		}
		// load back curretnDir
		currentDir = oldCurrent;
		return return_point;
	}

	VPoint return_point_fully_qualified(final String path)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> return_point_fully_qualified called with path:" + path);
		}
		// save currentDir
		if (currentDir == rootDir && path.equals("/")) {
			return rootDir;
		}
		final VPoint oldCurrent = currentDir;
		currentDir = rootDir;
		final String newPointName = resolvePath(path);
		if (newPointName == null) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.out.println("Could not resolve path");
			}
			return null;
		}
		final VPoint return_point = returnPointInCurrentDir(newPointName);
		// load back curretnDir
		currentDir = oldCurrent;
		return return_point;
	}

	String returnAbsolutePointName(final String path)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> returnAbsolutePointName called with path:" + path);
		}
		final String[] splitted = path.split("/");
		return splitted[splitted.length - 1];
	}

	VPoint returnPointInCurrentDir(final String exisingPointName)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> returnPointInCurrentDir called with path:" + exisingPointName);
		}
		if (exisingPointName == null || exisingPointName.equals("") || exisingPointName.length() == 0) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println("A point cannot exist with <blank> name");
			}
			return null;
		}
		else if (currentDir.searchForChildPoint(exisingPointName) == false) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println(currentDir.name + " does not contain " + exisingPointName);
			}
			return null;
		}
		else {
			final VPoint toBeReturned = currentDir.returnSubPoint(exisingPointName);
			return toBeReturned;
		}
	}

	@Override
	public String toString()
	{
		return "Root>" + rootDir.toString() + "\n CurrentDir: " + currentDir.toString() + "-- Open: " + openFileMap.toString();
	}

	/*
	 * VPoint traverseDownPath(final String originalPath) { final String[] pathArr = originalPath.split("/"); if (pathArr.length
	 * == 1) { if (VickyFS.DEBUG_MODE_ON) System.out.println("Nothing to resolve"); return currentDir; } else { VPoint
	 * parentAtPath = currentDir; for (int i = 0; i < pathArr.length - 1; i++) { final VPoint child =
	 * parentAtPath.returnSubPoint(pathArr[i]); if (child == null || child.isFile()) { if (VickyFS.DEBUG_MODE_ON)
	 * System.out.println("Could not resolve " + pathArr[i] + " in " + parentAtPath.name); return null; } else { parentAtPath =
	 * child; } } if (VickyFS.DEBUG_MODE_ON) System.out.println("Resolved. Returning " + parentAtPath.name); return
	 * parentAtPath; } }
	 */
	boolean traverseToNewDirInCurrentDir(final String exisingPointName)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> traverseToNewDirInCurrentDir called with path:" + exisingPointName);
		}
		if (exisingPointName == null || exisingPointName.equals("") || exisingPointName.length() == 0) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println("A directory cannot exist with <blank> name");
			}
			return false;
		}
		else if (exisingPointName.equals("..")) {
			return traverseUpOneLevel();
		}
		else if (currentDir.searchForChildPoint(exisingPointName) == false) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println(currentDir.name + " does not  contains " + exisingPointName);
			}
			return false;
		}
		else {
			final VPoint toBeReturned = currentDir.returnSubPoint(exisingPointName);
			if (toBeReturned.isFile()) {
				if (VickyFS.DEBUG_MODE_ON) {
					System.err.println(toBeReturned.name + " is not a directory...");
				}
				return false;
			}
			else {
				currentDir = toBeReturned;
				// if (VickyFS.DEBUG_MODE_ON) System.out.println("Entered " + toBeReturned.name);
				return true;
			}
		}
	}

	boolean traverseUpOneLevel()
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> traverseUpOneLevel called with <>");
		}
		if (currentDir.parentPoint != null) {
			currentDir = currentDir.parentPoint;
			return true;
		}
		else {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println("Reached root or parentless point");
			}
			return false;
		}
	}

	int vread(final int fd, final int size, final int offset, final ByteBuffer buf)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> vread called with fd:" + fd + " and " + size + " bytes");
		}
		if (openFileMap.containsKey(fd)) {
			final VPoint file = openFileMap.get(fd);
			final int lengthRead = Math.min(offset + size, file.contents.size());
			final Byte[] bigByteArr = file.contents.subList(offset, lengthRead).toArray(new Byte[] {});
			if (VickyFS.DEBUG_MODE_ON) {
				System.out.println(bigByteArr.length + " bytes read from " + file.name);
			}
			buf.put(getSmall(bigByteArr));
			return bigByteArr.length;
		}
		else {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println("No Such FD Mapped to an Open File");
			}
			return -ErrorCodes.EBADF();
		}
	}

	int vwrite(final int fd, final int size, final int offset, final ByteBuffer buf)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("  >>> vwrite called with fd:" + fd + " and size: " + size);
		}
		if (openFileMap.containsKey(fd)) {
			if (getSpaceOf(offset + size) == false) {
				if (VickyFS.DEBUG_MODE_ON) {
					System.err.println("Ran out of space.");
				}
				return -ErrorCodes.ENOMEM();
			}
			final byte[] byteArr = new byte[size];
			buf.get(byteArr);
			final VPoint file = openFileMap.get(fd);
			final int newOffset = Math.min(offset, file.contents.size());
			final int spaceRecovered = file.contents.size() - newOffset;
			if (spaceRecovered > 0) {
				recoverSpaceOf(spaceRecovered);
			}
			file.contents.removeAll(file.contents.subList(newOffset, file.contents.size()));
			file.contents.addAll(newOffset, Arrays.asList(getLarge(byteArr)));
			if (VickyFS.DEBUG_MODE_ON) {
				System.out.println(file.name + " now contains :" + file.contents.size() + " bytes");
			}
			return size;
		}
		else {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println("No Such FD Mapped to an Open File");
			}
			return 0;
		}
	}
}

class VPoint
{
	static final boolean IS_FILE = false;
	static final boolean IS_DIRECTORY = true;
	static final int INIT_CONTENT_SIZE = 4096;
	String name;
	boolean pointType;
	HashSet<VPoint> childpoints;
	ArrayList<Byte> contents;
	VPoint parentPoint;

	VPoint(final String name)
	{
		this.name = name;
	}

	VPoint(final String name, final boolean pointType, final VPoint parentPoint)
	{
		this.name = name;
		this.pointType = pointType;
		this.parentPoint = parentPoint;
		if (isDirectory()) {
			childpoints = new HashSet<VPoint>();
		}
		else {
			contents = new ArrayList<Byte>(VPoint.INIT_CONTENT_SIZE);
		}
	}

	int addChildToDir(final VPoint childPoint)
	{
		if (pointType == VPoint.IS_FILE) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println(name + " is a File. Cannot add " + childPoint.name + " to it.");
			}
			return -ErrorCodes.ENOTDIR();
		}
		else {
			if (childpoints.contains(childPoint)) {
				if (VickyFS.DEBUG_MODE_ON) {
					System.err.println(name + " already contains " + childPoint.name + " in it.");
				}
				return -ErrorCodes.EEXIST();
			}
			if (childPoint.parentPoint.name != name) {
				if (VickyFS.DEBUG_MODE_ON) {
					System.err.println("Parents don't match.. Not adding..");
				}
				return -ErrorCodes.ECANCELED();
			}
			childpoints.add(childPoint);
			return 0;
		}
	}

	@Override
	public boolean equals(final Object o)
	{
		if (o instanceof VPoint == false) {
			return false;
		}
		return name.contentEquals(((VPoint) o).name);
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	boolean isDirectory()
	{
		return pointType == VPoint.IS_DIRECTORY;
	}

	boolean isFile()
	{
		return pointType == VPoint.IS_FILE;
	}

	boolean removeChildFromDir(final VPoint childPoint)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(name);
		}
		if (childpoints.contains(childPoint)) {
			childpoints.remove(childPoint);
			return true;
		}
		return false;
	}

	ArrayList<String> returnChildPoints()
	{
		final ArrayList<String> retList = new ArrayList<String>(childpoints.size());
		final Iterator<VPoint> iter = childpoints.iterator();
		for (final VPoint childPoint : childpoints) {
			retList.add(childPoint.name);
		}
		return retList;
	}

	VPoint returnSubPoint(final String childName)
	{
		if (isFile()) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println(name + " is a File. It cannot contain " + childName + " in it.");
			}
			return null;
		}
		else {
			final VPoint proxyPoint = new VPoint(childName);
			if (childpoints.contains(proxyPoint) == false) {
				return null;
			}
			else {
				final ArrayList<VPoint> list = new ArrayList<VPoint>(childpoints);
				return list.get(list.indexOf(proxyPoint));
			}
		}
	}

	boolean searchForChildPoint(final String childName)
	{
		if (isFile()) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println(name + " is a File. It cannot contain " + childName + " in it.");
			}
			return false;
		}
		else {
			return childpoints.contains(new VPoint(childName));
		}
	}

	@Override
	public String toString()
	{
		String ret = "Point > " + name + " > " + ((pointType == VPoint.IS_DIRECTORY) ? "Directory" : "File");
		if (isDirectory()) {
			ret = ret + " - Children: " + childpoints.toString();
		}
		else {
			ret = ret + " - Contents: " + contents;
		}
		return ret;
	}
}
