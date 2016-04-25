package net.vicky;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.FlockCommand;
import net.fusejna.FuseException;
import net.fusejna.StructFlock.FlockWrapper;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.StructStatvfs.StatvfsWrapper;
import net.fusejna.StructTimeBuffer.TimeBufferWrapper;
import net.fusejna.XattrFiller;
import net.fusejna.XattrListFiller;
import net.fusejna.types.TypeMode.ModeWrapper;
import net.fusejna.types.TypeMode.NodeType;

public class VRamdiskPersistent extends net.fusejna.FuseFilesystem
{
	public static void main(final String[] args) throws FuseException
	{
		System.out.println("You called with " + Arrays.toString(args));
		if (args.length < 2) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println("Usage: ramdisk <mountpoint> <size>");
			}
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println("You gave wrongly, " + Arrays.toString(args));
			}
			System.exit(1);
		}
		File persistenceFile;
		if (args.length == 3 && (persistenceFile = new File(args[2])).exists() && new File(args[2]).isFile()) {
			System.out.println("Reloading old Ramdisk at " + persistenceFile);
			new VRamdiskPersistent(args[2]).mount(args[0]);
		}
		else {
			System.out.println("Loading new Ramdisk");
			final long capacity = Long.parseLong(args[1]) * 1024 * 1024;
			System.out.println("Persistent Ramdisk of size " + capacity + " bytes loaded at " + args[0] + ".");
			new VRamdiskPersistent(capacity).mount(args[0]);
		}
	}

	VickyFS openVFS;

	VRamdiskPersistent(final long size)
	{
		openVFS = new VickyFS(size);
	}

	VRamdiskPersistent(final String path)
	{
		deserialize(path);
		System.out.println("Persistent Ramdisk of size " + openVFS.size + " bytes REloaded from file " + path + ".");
	}

	@Override
	public int access(final String path, final int access)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- access unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void afterUnmount(final File mountPoint)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- afterUnmount unimplemented called --- ");
			// TODO Auto-generated method stub
		}
		final String path = System.getProperty("user.home") + File.separatorChar + "VickyRamdisk.persistence";
		System.out.println("Persistence file stored at " + path);
		serialize(path);
	}

	@Override
	public void beforeMount(final File mountPoint)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- beforeMount unimplemented called --- ");
			// TODO Auto-generated method stub
		}
	}

	@Override
	public int bmap(final String path, final FileInfoWrapper info)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- bmap unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int chmod(final String path, final ModeWrapper mode)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- chmod unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int chown(final String path, final long uid, final long gid)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- chown unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int create(final String path, final ModeWrapper mode, final FileInfoWrapper info)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(
					"===============  create called with " + path + " & fd: " + info.fh() + " on " + openVFS.rootDir.name);
		}
		final int existing = open(path, info);
		if (existing < 0) {
			final int mknod_result = mknod(path, mode, 0);
			if (mknod_result == 0) {
				return open(path, info);
			}
			else {
				if (VickyFS.DEBUG_MODE_ON) {
					System.err.println("mknod failed with " + mknod_result);
				}
				return mknod_result;
			}
		}
		else {
			return -ErrorCodes.EEXIST();
		}
	}

	void deserialize(final String path)
	{
		try {
			final FileInputStream fileIn = new FileInputStream(path);
			final ObjectInputStream in = new ObjectInputStream(fileIn);
			openVFS = (VickyFS) in.readObject();
			in.close();
			fileIn.close();
		}
		catch (final IOException i) {
			i.printStackTrace();
			return;
		}
		catch (final ClassNotFoundException c) {
			System.out.println("VickyFS class not found");
			c.printStackTrace();
			return;
		}
	}

	@Override
	public void destroy()
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- destroy unimplemented called --- ");
			// TODO Auto-generated method stub
		}
	}

	@Override
	public int fgetattr(final String path, final StatWrapper stat, final FileInfoWrapper info)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("===============  fgetattr called with " + path + " fd: " + info.fh());
		}
		return getattr(path, stat);
	}

	@Override
	public int flush(final String path, final FileInfoWrapper info)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("===============  flush called with " + path);
		}
		// if (openVFS.close_file_point((int) info.fh()) == true) {
		// return 0;
		// }
		return 0;
	}

	@Override
	public int fsync(final String path, final int datasync, final FileInfoWrapper info)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- fsync unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int fsyncdir(final String path, final int datasync, final FileInfoWrapper info)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- fsyncdir unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ftruncate(final String path, final long offset, final FileInfoWrapper info)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- ftruncate unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getattr(final String path, final StatWrapper stat)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("===============  getattr called with " + path);
		}
		VPoint point;
		if (path.equals("/") && openVFS.currentDir.name.equals("/")) {
			point = openVFS.currentDir;
		}
		else {
			point = openVFS.return_point_fully_qualified(path);
		}
		if (point == null) {
			return -ErrorCodes.ENOENT();
		}
		stat.ino(point.hashCode());
		if (point.isDirectory()) {
			stat.nlink(2);
			stat.size(point.name.length());
			stat.setMode(NodeType.DIRECTORY);
		}
		else {
			stat.nlink(1);
			stat.size(point.name.length() + point.contents.size());
			stat.setMode(NodeType.FILE);
		}
		return 0;
	}

	@Override
	protected String getName()
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- getName unimplemented called --- ");
		}
		return "Vicky's Ramdisk";
	}

	@Override
	protected String[] getOptions()
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- getOptions unimplemented called --- ");
		}
		return new String[] { "-s" };
	}

	@Override
	public int getxattr(final String path, final String xattr, final XattrFiller filler, final long size, final long position)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- getxattr unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void init()
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- init unimplemented called --- ");
		}
	}

	@Override
	public int link(final String path, final String target)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- link unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int listxattr(final String path, final XattrListFiller filler)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- listxattr unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int lock(final String path, final FileInfoWrapper info, final FlockCommand command, final FlockWrapper flock)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- lock unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int mkdir(final String path, final ModeWrapper mode)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("===============  mkdir called with " + path);
		}
		return openVFS.create_point(path, VPoint.IS_DIRECTORY);
	}

	@Override
	public int mknod(final String path, final ModeWrapper mode, final long dev)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("===============  mknod called with " + path);
		}
		return openVFS.create_point(path, VPoint.IS_FILE);
	}

	@Override
	public int open(final String path, final FileInfoWrapper info)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(
					"===============  open called with " + path + "fd: " + info.fh() + " on " + openVFS.currentDir.name);
		}
		final int existingFD = openVFS.open_file(path);
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("Open fuse: existingFD " + existingFD);
		}
		if (existingFD < 0) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.out.println("open Returngin " + existingFD);
			}
			return existingFD;
		}
		else {
			if (VickyFS.DEBUG_MODE_ON) {
				System.out.println("open Returngin success " + 0);
			}
			info.fh(existingFD);
			return 0;
		}
	}

	@Override
	public int opendir(final String path, final FileInfoWrapper info)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("===============  opendir called with " + path + " and fh: " + info.fh());
		}
		return openVFS.change_dir(path) == true ? 0 : -1;
	}

	@Override
	public int read(final String path, final ByteBuffer buffer, final long size, final long offset, final FileInfoWrapper info)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("===============  read called with " + path);
		}
		return openVFS.vread((int) info.fh(), (int) size, (int) offset, buffer);
	}

	@Override
	public int readdir(final String path, final DirectoryFiller filler)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("===============  readdir called with " + path);
		}
		final VPoint toBeRead = openVFS.return_point(path);
		if (toBeRead == null) {
			if (VickyFS.DEBUG_MODE_ON) {
				System.err.println("No file returned by return_point: " + path);
			}
			return -ErrorCodes.ENOENT();
		}
		else if (toBeRead.isFile()) {
			return -ErrorCodes.ENOTDIR();
		}
		else {
			filler.add(toBeRead.returnChildPoints());
			return 0;
		}
	}

	@Override
	public int readlink(final String path, final ByteBuffer buffer, final long size)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- readlink unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int release(final String path, final FileInfoWrapper info)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- release unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int releasedir(final String path, final FileInfoWrapper info)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- releasedir unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int removexattr(final String path, final String xattr)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- removexattr unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int rename(final String path, final String newName)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- rename unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int rmdir(final String path)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("===============  rmdir called with " + path);
		}
		return openVFS.remove_point(path) == true ? 0 : -1;
	}

	void serialize(final String path)
	{
		try {
			final FileOutputStream fileOut = new FileOutputStream(path);
			final ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(openVFS);
			out.close();
			fileOut.close();
			System.out.println("Serialized data is saved in " + path);
		}
		catch (final IOException i) {
			i.printStackTrace();
		}
	}

	@Override
	public int setxattr(final String path, final String xattr, final ByteBuffer value, final long size, final int flags,
			final int position)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- setxattr unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int statfs(final String path, final StatvfsWrapper wrapper)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- statfs unimplemented called --- ");
		}
		wrapper.set(1, 1, openVFS.remaining_space, openVFS.remaining_space, openVFS.size, openVFS.remaining_space,
				openVFS.remaining_space, openVFS.size);
		return 0;
	}

	@Override
	public int symlink(final String path, final String target)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- symlink unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int truncate(final String path, final long offset)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- truncate unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int unlink(final String path)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("===============  unlink called with " + path);
		}
		return openVFS.remove_point(path) == true ? 0 : -1;
	}

	@Override
	public int utimens(final String path, final TimeBufferWrapper wrapper)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println(" --- utimens unimplemented called --- ");
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int write(final String path, final ByteBuffer buf, final long bufSize, final long writeOffset,
			final FileInfoWrapper info)
	{
		if (VickyFS.DEBUG_MODE_ON) {
			System.out.println("===============  write called with " + path);
		}
		return openVFS.vwrite((int) info.fh(), (int) bufSize, (int) writeOffset, buf);
	}
}
