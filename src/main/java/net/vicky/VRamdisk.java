package net.vicky;

import java.io.File;
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

public class VRamdisk extends net.fusejna.FuseFilesystem
{
	public static void main(final String[] args) throws FuseException
	{
		if (args.length < 2) {
			System.err.println("Usage: ramdisk <mountpoint> <size>");
			System.err.println("You gave wrongly, " + Arrays.toString(args));
			System.exit(1);
		}
		final int capacity = Integer.parseInt(args[1]) * 1024 * 1024;
		System.out.println("Ramdisk of size " + capacity + " bytes loaded at " + args[0] + ".");
		new VRamdisk(capacity).mount(args[0]);
	}

	VickyFS openVFS;

	VRamdisk(final int size)
	{
		openVFS = new VickyFS(size);
	}

	@Override
	public int access(final String path, final int access)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void afterUnmount(final File mountPoint)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void beforeMount(final File mountPoint)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public int bmap(final String path, final FileInfoWrapper info)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int chmod(final String path, final ModeWrapper mode)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int chown(final String path, final long uid, final long gid)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int create(final String path, final ModeWrapper mode, final FileInfoWrapper info)
	{
		System.out.println("create called with " + path + " on " + openVFS);
		return open(path, info);
	}

	@Override
	public void destroy()
	{
		// TODO Auto-generated method stub
	}

	@Override
	public int fgetattr(final String path, final StatWrapper stat, final FileInfoWrapper info)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int flush(final String path, final FileInfoWrapper info)
	{
		System.out.println("flush called with " + path);
		if (openVFS.close_file_point((int) info.fh()) == true) {
			return 0;
		}
		return -1;
	}

	@Override
	public int fsync(final String path, final int datasync, final FileInfoWrapper info)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int fsyncdir(final String path, final int datasync, final FileInfoWrapper info)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ftruncate(final String path, final long offset, final FileInfoWrapper info)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getattr(final String path, final StatWrapper stat)
	{
		System.out.println("getattr called with " + path);
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
			stat.size(point.name.length() * 2);
			stat.setMode(NodeType.DIRECTORY);
		}
		else {
			stat.nlink(1);
			stat.size(point.name.length() * 2 + point.contents.size());
			stat.setMode(NodeType.FILE);
		}
		return 0;
	}

	@Override
	protected String getName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String[] getOptions()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getxattr(final String path, final String xattr, final XattrFiller filler, final long size, final long position)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void init()
	{
	}

	@Override
	public int link(final String path, final String target)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int listxattr(final String path, final XattrListFiller filler)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int lock(final String path, final FileInfoWrapper info, final FlockCommand command, final FlockWrapper flock)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int mkdir(final String path, final ModeWrapper mode)
	{
		System.out.println("mkdir called with " + path);
		return openVFS.create_point(path, VPoint.IS_DIRECTORY);
	}

	@Override
	public int mknod(final String path, final ModeWrapper mode, final long dev)
	{
		System.out.println("mknod called with " + path);
		return openVFS.create_point(path, VPoint.IS_FILE);
	}

	@Override
	public int open(final String path, final FileInfoWrapper info)
	{
		System.out.println("open called with " + path + " on " + openVFS);
		final int existingFD = openVFS.open_file(path);
		if (existingFD != -1) {
			info.fh(existingFD);
			return 0;
		}
		else {
			openVFS.create_point(path, VPoint.IS_FILE);
			final int newFD = openVFS.open_file(path);
			info.fh(newFD);
			return 0;
		}
	}

	@Override
	public int opendir(final String path, final FileInfoWrapper info)
	{
		System.out.println("opendir called with " + path);
		return openVFS.change_dir(path) == true ? 0 : -1;
	}

	@Override
	public int read(final String path, final ByteBuffer buffer, final long size, final long offset, final FileInfoWrapper info)
	{
		System.out.println("read called with " + path);
		return openVFS.vread((int) info.fh(), (int) size, (int) offset, buffer);
	}

	@Override
	public int readdir(final String path, final DirectoryFiller filler)
	{
		System.out.println("readdir called with " + path);
		final VPoint toBeRead = openVFS.return_point(path);
		if (toBeRead == null) {
			System.err.println("No file returned by return_point: " + path);
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int release(final String path, final FileInfoWrapper info)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int releasedir(final String path, final FileInfoWrapper info)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int removexattr(final String path, final String xattr)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int rename(final String path, final String newName)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int rmdir(final String path)
	{
		System.out.println("rmdir called with " + path);
		return openVFS.remove_point(path) == true ? 0 : -1;
	}

	@Override
	public int setxattr(final String path, final String xattr, final ByteBuffer value, final long size, final int flags,
			final int position)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int statfs(final String path, final StatvfsWrapper wrapper)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int symlink(final String path, final String target)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int truncate(final String path, final long offset)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int unlink(final String path)
	{
		System.out.println("unlink called with " + path);
		return openVFS.remove_point(path) == true ? 0 : -1;
	}

	@Override
	public int utimens(final String path, final TimeBufferWrapper wrapper)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int write(final String path, final ByteBuffer buf, final long bufSize, final long writeOffset,
			final FileInfoWrapper info)
	{
		System.out.println("write called with " + path);
		return openVFS.vwrite((int) info.fh(), (int) bufSize, (int) writeOffset, buf);
	}
}
