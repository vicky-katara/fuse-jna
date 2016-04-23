package net.vicky;

import java.io.File;
import java.nio.ByteBuffer;

import net.fusejna.DirectoryFiller;
import net.fusejna.FlockCommand;
import net.fusejna.StructFlock.FlockWrapper;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.StructStatvfs.StatvfsWrapper;
import net.fusejna.StructTimeBuffer.TimeBufferWrapper;
import net.fusejna.XattrFiller;
import net.fusejna.XattrListFiller;
import net.fusejna.types.TypeMode.ModeWrapper;

public class VRamdisk extends net.fusejna.FuseFilesystem
{
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
		// TODO Auto-generated method stub
		return 0;
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
		// TODO Auto-generated method stub
		return 0;
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
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int mknod(final String path, final ModeWrapper mode, final long dev)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int open(final String path, final FileInfoWrapper info)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int opendir(final String path, final FileInfoWrapper info)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int read(final String path, final ByteBuffer buffer, final long size, final long offset, final FileInfoWrapper info)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int readdir(final String path, final DirectoryFiller filler)
	{
		// TODO Auto-generated method stub
		return 0;
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
		// TODO Auto-generated method stub
		return 0;
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
		// TODO Auto-generated method stub
		return 0;
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
		// TODO Auto-generated method stub
		return 0;
	}
}
