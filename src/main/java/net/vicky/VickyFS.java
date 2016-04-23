package net.vicky;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/*
 * struct VPoint { char* name; int isDirectory; char* contents; int current_content_capacity; struct Queue *subQueue; struct
 * VPoint *parent; struct OpenVPoint *openContainerForThis;
 */
public class VickyFS
{
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
		final VickyFS firstVFS = new VickyFS(200);
		firstVFS.create_point("dirA", VPoint.IS_DIRECTORY);
		firstVFS.create_point("dirA/dirA1", VPoint.IS_DIRECTORY);
		firstVFS.create_point("dirA/dirA1/dirA11", VPoint.IS_DIRECTORY);
		// System.out.println(firstVFS);
		firstVFS.create_point("dirA/dirA1/dirA11/fileA11a", VPoint.IS_FILE);
		final int A11aFD = firstVFS.open_file("dirA/dirA1/dirA11/fileA11a");
		final byte[] bArr = new byte[] { 1, 20, 3, 99 };
		firstVFS.vwrite(A11aFD, bArr.length, 0, ByteBuffer.wrap(bArr));
		// reading
		final ByteBuffer readBuf = ByteBuffer.wrap(new byte[100]);
		firstVFS.vread(A11aFD, readBuf.remaining() - 1, 0, readBuf);
		final byte[] readArr = readBuf.array();
		System.out.println("fileA11a contains: " + Arrays.toString(readArr));
	}

	VPoint rootDir;
	VPoint currentDir;
	HashMap<Integer, VPoint> openFileMap;
	int lastAllocatedFD;
	// String current_path_str;
	int size, remaining_space;

	VickyFS(final int size)
	{
		lastAllocatedFD = -1;
		rootDir = new VPoint("/", VPoint.IS_DIRECTORY, null);
		currentDir = rootDir;
		openFileMap = new HashMap<Integer, VPoint>();
		// current_path_str = "/";
		this.size = size;
		remaining_space = size;
	}

	boolean addSpaceOf(final int newDataSize)
	{
		final int newSpaceNeeded = newDataSize * 2; // char is 2 bytes
		if (newSpaceNeeded < remaining_space) {
			remaining_space -= newSpaceNeeded;
			System.out.println("New space added: " + newSpaceNeeded + " Remaining: " + remaining_space + " Total: " + size);
			return true;
		}
		else {
			System.out.println("New space added: " + newSpaceNeeded + " Remaining: " + remaining_space + " Total: " + size);
			return false;
		}
	}

	boolean change_dir(final String path)
	{
		final String remainingPath = resolvePath(path);
		if (traverseToNewDirInCurrentDir(remainingPath) == true) {
			return true;
		}
		else {
			return false;
		}
	}

	boolean close_file_point(final int fd)
	{
		if (openFileMap.containsKey(fd)) {
			openFileMap.remove(fd);
			return true;
		}
		else {
			System.err.println("No Such FD Mapped to an Open File");
			return false;
		}
	}

	boolean create_point(final String path, final boolean type)
	{
		// save currentDir
		final VPoint oldCurrent = currentDir;
		final String newPointName = resolvePath(path);
		final boolean success = currentDir.addChildToDir(new VPoint(newPointName, type, currentDir));
		// load back curretnDir
		currentDir = oldCurrent;
		return success;
	}

	boolean createNewPointUnderCurrentDir(final String newPointName, final boolean type)
	{
		if (newPointName == null || newPointName.equals("") || newPointName.length() == 0) {
			System.err.println("A point cannot be created with <blank> name");
			return false;
		}
		else if (currentDir.searchForChildPoint(newPointName) == true) {
			System.err.println(currentDir.name + " already  contains " + newPointName + " .");
			return false;
		}
		else {
			final VPoint newVPoint = new VPoint(newPointName, type, currentDir);
			if (generateSpaceFor(newVPoint) == true) {
				System.out.println(newVPoint.name + " added under " + currentDir.name);
				currentDir.addChildToDir(newVPoint);
				return true;
			}
			else {
				System.err.println("Not enough space!!");
				return false;
			}
		}
	}

	boolean createPoint(final String path, final boolean type)
	{
		// save currentDir
		final VPoint oldCurrent = currentDir;
		final String newPointName = resolvePath(path);
		final boolean success = currentDir.addChildToDir(new VPoint(newPointName, type, currentDir));
		// load back curretnDir
		currentDir = oldCurrent;
		return success;
	}

	boolean generateSpaceFor(final VPoint newPoint)
	{
		int newSpaceNeeded = newPoint.name.length() * 2; // char is 2 bytes
		if (newPoint.isFile()) {
			newSpaceNeeded += newPoint.contents.size();
		}
		if (newSpaceNeeded < remaining_space) {
			remaining_space -= newSpaceNeeded;
			System.out.println("New space added: " + newSpaceNeeded + " Remaining: " + remaining_space + " Total: " + size);
			return true;
		}
		else {
			System.out.println("New space added: " + newSpaceNeeded + " Remaining: " + remaining_space + " Total: " + size);
			return false;
		}
	}

	int open_file(final String path)
	{
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
		if (openFileMap.containsValue(new VPoint(filePointName))) {
			for (final Map.Entry<Integer, VPoint> entry : openFileMap.entrySet()) {
				final VPoint existingPoint = entry.getValue();
				if (existingPoint.name.equals(filePointName) && existingPoint.parentPoint == currentDir) {
					System.err.println(existingPoint.name + " was already open.");
					return entry.getKey();
				}
			}
		}
		if (filePointName == null || filePointName.equals("") || filePointName.length() == 0) {
			System.err.println("A point cannot be created with <blank> name");
			return -1;
		}
		else if (currentDir.searchForChildPoint(filePointName) == true) {
			final VPoint toBeOpened = currentDir.returnSubPoint(filePointName);
			if (toBeOpened.isDirectory()) {
				System.err.println(toBeOpened.name + " is not a file. Cannot open...");
				return -1;
			}
			else {
				openFileMap.put(++lastAllocatedFD, toBeOpened);
				return lastAllocatedFD;
			}
		}
		System.err.println("Should not be here. openFileInCurrentDir.");
		return -1;
	}

	void recoverSpaceFor(final VPoint removalPoint)
	{
		int spaceRecovered = removalPoint.name.length() * 2; // char is 2 bytes
		if (removalPoint.isFile()) {
			spaceRecovered += removalPoint.contents.size();
		}
		remaining_space += spaceRecovered;
		System.out.println("New space added: " + spaceRecovered + " Remaining: " + remaining_space + " Total: " + size);
	}

	boolean remove_point(final String path)
	{
		boolean success;
		// save currentDir
		final VPoint oldCurrent = currentDir;
		final String newPointName = resolvePath(path);
		final VPoint toBeRemoved = currentDir.returnSubPoint(newPointName);
		if (toBeRemoved == null) {
			System.err.println("Could not find " + newPointName + " inside " + currentDir);
			success = false;
		}
		else {
			success = toBeRemoved.removeChildFromDir(toBeRemoved);
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
		if (exisingPointName == null || exisingPointName.equals("") || exisingPointName.length() == 0) {
			System.err.println("A point cannot exist with <blank> name");
			return false;
		}
		else if (currentDir.searchForChildPoint(exisingPointName) == false) {
			System.err.println(currentDir.name + " does not  contains " + exisingPointName + " .");
			return false;
		}
		else {
			final VPoint toBeRemoved = currentDir.returnSubPoint(exisingPointName);
			recoverSpaceFor(toBeRemoved);
			System.out.println(toBeRemoved.name + " removed from under " + currentDir.name);
			currentDir.removeChildFromDir(toBeRemoved);
			return true;
		}
	}

	String resolvePath(final String originalPath)
	{
		final String[] pathArr = originalPath.split("/");
		if (pathArr.length == 1) {
			System.out.println("Nothing to resolve");
			return originalPath;
		}
		else {
			for (int i = 0; i < pathArr.length - 1; i++) {
				if (traverseToNewDirInCurrentDir(pathArr[i]) == false) {
					return null;
				}
			}
			return pathArr[pathArr.length - 1];
		}
	}

	VPoint return_point(final String path)
	{
		// save currentDir
		final VPoint oldCurrent = currentDir;
		final String newPointName = resolvePath(path);
		final VPoint return_point = returnPointInCurrentDir(newPointName);
		// load back curretnDir
		currentDir = oldCurrent;
		return return_point;
	}

	String returnAbsolutePointName(final String path)
	{
		final String[] splitted = path.split("/");
		return splitted[splitted.length - 1];
	}

	VPoint returnPointInCurrentDir(final String exisingPointName)
	{
		if (exisingPointName == null || exisingPointName.equals("") || exisingPointName.length() == 0) {
			System.err.println("A point cannot exist with <blank> name");
			return null;
		}
		else if (currentDir.searchForChildPoint(exisingPointName) == false) {
			System.err.println(currentDir.name + " does not  contains " + exisingPointName + " .");
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
		return "Root>" + rootDir.toString() + "\n CurrentDir: " + currentDir.toString();
	}

	/*
	 * VPoint traverseDownPath(final String originalPath) { final String[] pathArr = originalPath.split("/"); if (pathArr.length
	 * == 1) { System.out.println("Nothing to resolve"); return currentDir; } else { VPoint parentAtPath = currentDir; for (int
	 * i = 0; i < pathArr.length - 1; i++) { final VPoint child = parentAtPath.returnSubPoint(pathArr[i]); if (child == null ||
	 * child.isFile()) { System.out.println("Could not resolve " + pathArr[i] + " in " + parentAtPath.name); return null; } else
	 * { parentAtPath = child; } } System.out.println("Resolved. Returning " + parentAtPath.name); return parentAtPath; } }
	 */
	boolean traverseToNewDirInCurrentDir(final String exisingPointName)
	{
		if (exisingPointName == null || exisingPointName.equals("") || exisingPointName.length() == 0) {
			System.err.println("A directory cannot exist with <blank> name");
			return false;
		}
		else if (exisingPointName.equals("..")) {
			return traverseUpOneLevel();
		}
		else if (currentDir.searchForChildPoint(exisingPointName) == false) {
			System.err.println(currentDir.name + " does not  contains " + exisingPointName);
			return false;
		}
		else {
			final VPoint toBeReturned = currentDir.returnSubPoint(exisingPointName);
			if (toBeReturned.isFile()) {
				System.err.println(toBeReturned.name + " is not a directory...");
				return false;
			}
			else {
				currentDir = toBeReturned;
				// System.out.println("Entered " + toBeReturned.name);
				return true;
			}
		}
	}

	boolean traverseUpOneLevel()
	{
		if (currentDir.parentPoint != null) {
			currentDir = currentDir.parentPoint;
			return true;
		}
		else {
			System.err.println("Reached root or parentless point");
			return false;
		}
	}

	boolean vread(final int fd, final int size, final int offset, final ByteBuffer buf)
	{
		if (openFileMap.containsKey(fd)) {
			final VPoint file = openFileMap.get(fd);
			final int lengthRead = Math.min(offset + size, file.contents.size());
			final Byte[] bigByteArr = file.contents.subList(offset, lengthRead).toArray(new Byte[] {});
			System.out.println(Arrays.toString(bigByteArr) + " read from " + file.name);
			buf.put(getSmall(bigByteArr));
			return true;
		}
		else {
			System.err.println("No Such FD Mapped to an Open File");
			return false;
		}
	}

	boolean vwrite(final int fd, final int size, final int offset, final ByteBuffer buf)
	{
		if (openFileMap.containsKey(fd)) {
			if (addSpaceOf(offset + size) == false) {
				System.err.println("Ran out of space.");
				return false;
			}
			final byte[] byteArr = new byte[size];
			buf.get(byteArr);
			final VPoint file = openFileMap.get(fd);
			file.contents.addAll(offset, Arrays.asList(getLarge(byteArr)));
			System.out.println(file.name + " now contains :" + file.contents);
			return true;
		}
		else {
			System.err.println("No Such FD Mapped to an Open File");
			return false;
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

	boolean addChildToDir(final VPoint childPoint)
	{
		if (pointType == VPoint.IS_FILE) {
			System.err.println(name + " is a File. Cannot add " + childPoint.name + " to it.");
			return false;
		}
		else {
			if (childpoints.contains(childPoint)) {
				System.err.println(name + " already contains " + childPoint.name + " in it.");
				return false;
			}
			if (childPoint.parentPoint.name != name) {
				System.err.println("Parents don't match.. Not adding..");
				return false;
			}
			childpoints.add(childPoint);
			return true;
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
		if (isFile()) {
			System.err.println(name + " is a File. Cannot remove " + childPoint.name + " to it.");
			return false;
		}
		else {
			if (childpoints.contains(childPoint)) {
				childpoints.remove(childPoint);
				return true;
			}
			return false;
		}
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
			System.err.println(name + " is a File. It cannot contain " + childName + " in it.");
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
			System.err.println(name + " is a File. It cannot contain " + childName + " in it.");
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
