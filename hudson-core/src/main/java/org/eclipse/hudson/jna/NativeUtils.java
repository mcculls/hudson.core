/**
 * *****************************************************************************
 *
 * Copyright (c) 2011, Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   Winston Prakash
 *
 ******************************************************************************
 */
package org.eclipse.hudson.jna;

import hudson.model.Descriptor;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Native Support Utility (singleton) that delegates the actions to available
 * Native Support Extensions
 *
 * @since 3.0.0
 * @see NativeAccessSupport, ZfsSupport
 */
public class NativeUtils implements Serializable {

    private NativeZfsSupport nativeZfsSupport;
    private NativeUnixSupport nativeUnixSupport;
    private NativeWindowsSupport nativeWindowsSupport;
    private NativeMacSupport nativeMacSupport;
    private static Logger logger = LoggerFactory.getLogger(NativeUtils.class);

   public static NativeUtils INSTANCE;

    private NativeUtils() {
        try {
            List<NativeUnixSupport> nativeUnixSupports = getAvailableNativeUnixSupports();
            if (nativeUnixSupports.size() > 0) {
                nativeUnixSupport = nativeUnixSupports.get(0);
            }

            List<NativeWindowsSupport> nativeWindowsSupports = getAvailableNativeWindowSupports();
            if (nativeWindowsSupports.size() > 0) {
                nativeWindowsSupport = nativeWindowsSupports.get(0);
            }

            List<NativeMacSupport> nativeMacSupports = getAvailableNativeMacSupports();
            if (nativeMacSupports.size() > 0) {
                nativeMacSupport = nativeMacSupports.get(0);
            }
            List<NativeZfsSupport> nativeZfsSupports = getAvailableNativeZfsSupports();
            if (nativeZfsSupports.size() > 0) {
                nativeZfsSupport = nativeZfsSupports.get(0);
            }
        } catch (Exception exc) {
            logger.info("Error getting Native Support Extensions", exc);
        }

    }

    private List<NativeUnixSupport> getAvailableNativeUnixSupports() {
        List<NativeUnixSupport> nativeUnixSupports = new ArrayList<NativeUnixSupport>();
        if (NativeUnixSupport.all() != null && !NativeUnixSupport.all().isEmpty()) {
            for (Descriptor<NativeUnixSupport> nativeUnixSupport : NativeUnixSupport.all()) {
                try {
                    nativeUnixSupports.add(nativeUnixSupport.newInstance(null, null));
                } catch (Throwable exc) {
                    logger.info("Failed to instantiate Native Unix Support - " + nativeUnixSupport.getDisplayName());
                }
            }
        }
        return nativeUnixSupports;
    }

    private List<NativeWindowsSupport> getAvailableNativeWindowSupports() {
        List<NativeWindowsSupport> nativeWindowsSupports = new ArrayList<NativeWindowsSupport>();
        if (NativeWindowsSupport.all() != null && !NativeWindowsSupport.all().isEmpty()) {
            for (Descriptor<NativeWindowsSupport> nativeWindowsSupport : NativeWindowsSupport.all()) {
                try {
                    nativeWindowsSupports.add(nativeWindowsSupport.newInstance(null, null));
                } catch (Throwable exc) {
                    logger.info("Failed to instantiate Native Window Support - " + nativeWindowsSupport.getDisplayName());
                }
            }
        }
        return nativeWindowsSupports;
    }

    private List<NativeMacSupport> getAvailableNativeMacSupports() {
        List<NativeMacSupport> nativeMacSupports = new ArrayList<NativeMacSupport>();
        if (NativeMacSupport.all() != null && !NativeMacSupport.all().isEmpty()) {
            for (Descriptor<NativeMacSupport> nativeMacSupport : NativeMacSupport.all()) {
                try {
                    nativeMacSupports.add(nativeMacSupport.newInstance(null, null));
                } catch (Throwable exc) {
                    logger.info("Failed to instantiate Native Mac Support - " + nativeMacSupport.getDisplayName());
                }
            }
        }
        return nativeMacSupports;
    }

    private List<NativeZfsSupport> getAvailableNativeZfsSupports() {
        List<NativeZfsSupport> nativeZfsSupports = new ArrayList<NativeZfsSupport>();
        if (NativeZfsSupport.all() != null && !NativeZfsSupport.all().isEmpty()) {
            for (Descriptor<NativeZfsSupport> nativeZfsSupport : NativeZfsSupport.all()) {
                try {
                    nativeZfsSupports.add(nativeZfsSupport.newInstance(null, null));
                } catch (Throwable exc) {
                    logger.info("Failed to instantiate Native Zfs Support - " + nativeZfsSupport.getDisplayName());
                }
            }
        }
        return nativeZfsSupports;
    }

    /**
     * Delay the instance creation until needed, else slave ClassLoader deserialization will try to
     * instantiate the instance and will fail to load the extension points.
     * @return 
     */
    public static synchronized NativeUtils getInstance() {
        if (INSTANCE == null){
            INSTANCE = new NativeUtils();
        }
        return INSTANCE;
    }

    /**
     * Check if any Native Unix Support plugin is installed
     * @return
     */
    public boolean hasUnixSupport() {
        return nativeUnixSupport != null;
    }

    /**
     * Check if any Native Windows Support plugin is installed
     *
     * @return
     */
    public boolean hasWindowsSupport() {
        return nativeWindowsSupport != null;
    }

    /**
     * Check if any Native Mac Support plugin is installed
     *
     * @return
     */
    public boolean hasMacSupport() {
        return nativeMacSupport != null;
    }

    /**
     * Check if any Native ZFS Support plugin is installed
     *
     * @return
     */
    public boolean hasZfsSupport() {
        return nativeZfsSupport != null;
    }

    private void ensureUnixSupport(NativeFunction function) throws NativeAccessException {
        if (!hasUnixSupport()) {
            throw new NativeAccessException("Native Unix Support plugin not installed");
        }
        if (!nativeUnixSupport.hasSupportFor(function)) {
            throw new NativeAccessException("Installed Native Unix Support plugin does not support " + function);
        }
    }

    private void ensureWindowsSupport(NativeFunction function) throws NativeAccessException {
        if (!hasWindowsSupport()) {
            throw new NativeAccessException("Native Windows Support plugin not installed");
        }
        if (!nativeWindowsSupport.hasSupportFor(function)) {
            throw new NativeAccessException("Installed Native Windows Support plugin does not support " + function);
        }
    }

    private void ensureMacSupport(NativeFunction function) throws NativeAccessException {
        if (!hasMacSupport()) {
            throw new NativeAccessException("Native Mac Support plugin not installed");
        }
        if (!nativeMacSupport.hasSupportFor(function)) {
            throw new NativeAccessException("Installed Native Mac Support plugin does not support " + function);
        }
    }

    private void ensureZfsSupport(NativeFunction function) throws NativeAccessException {
        if (!hasZfsSupport()) {
            throw new NativeAccessException("Native ZFS Support plugin not installed");
        }
        if (!nativeZfsSupport.hasSupportFor(function)) {
            throw new NativeAccessException("Installed Native ZFS Support plugin does not support " + function);
        }
    }

    /**
     * Do the Unix style chmod (change file permission) on a File
     *
     * @param file
     * @param mask
     * @return true if the function executed successfully
     * @throws hudson.util.jna.Native.ExecutionError
     */
    public boolean chmod(File file, int mask) throws NativeAccessException {
        ensureUnixSupport(NativeFunction.CHMOD);
        return nativeUnixSupport.chmod(file, mask);
    }

    /**
     * Do the Unix style chown (change Owner permission) on a File
     *
     * @param file
     * @param mask
     * @return true if the function executed successfully
     * @throws hudson.util.jna.Native.ExecutionError
     */
    public boolean chown(File file, int uid, int gid) throws NativeAccessException {
        ensureUnixSupport(NativeFunction.CHOWN);
        return nativeUnixSupport.chown(file, uid, gid);

    }

    /**
     * Get the Unix style mode (file permission) of a file
     *
     * @param file
     * @return
     * @throws hudson.util.jna.Native.ExecutionError
     */
    public int mode(File file) throws NativeAccessException {
        ensureUnixSupport(NativeFunction.MODE);
        return nativeUnixSupport.mode(file);
    }

    /**
     * Get the name of the user who started this Java process
     *
     * @return
     * @throws hudson.util.jna.Native.NativeExecutionException
     */
    public String getProcessUser() throws NativeAccessException {
        ensureUnixSupport(NativeFunction.UNIX_USER);
        return nativeUnixSupport.getProcessUser();
    }

    /**
     * Make the file writable with native operation
     *
     * @param file
     * @return true if the operation is successful
     * @throws hudson.util.jna.Native.ExecutionError
     */
    public boolean makeFileWritable(File file) throws NativeAccessException {
        ensureUnixSupport(NativeFunction.FILE_WRITABLE);
        return nativeUnixSupport.makeFileWritable(file);
    }

    /**
     * Create Unix style symlink
     *
     * @param targetPath
     * @param file
     * @return true if the operation is successful
     * @throws hudson.util.jna.Native.ExecutionError
     */
    public boolean createSymlink(String targetPath, File file) throws NativeAccessException {
        ensureUnixSupport(NativeFunction.SYMLINK);
        return nativeUnixSupport.createSymlink(targetPath, file);
    }

    /**
     * Resolves symlink, if the given file is a symlink on a Unix System.
     * Otherwise return null.
     *
     * @param targetPath
     * @param file
     * @return String the resolved file path
     * @throws hudson.util.jna.Native.ExecutionError
     */
    public String resolveSymlink(File file) throws NativeAccessException {
        ensureUnixSupport(NativeFunction.RESOLVE_LINK);
        return nativeUnixSupport.resolveSymlink(file);
    }

    /**
     * Get the information about the System Memory
     *
     * @return
     * @throws hudson.util.jna.Native.ExecutionError
     */
    public NativeSystemMemory getSystemMemory() throws NativeAccessException {
        ensureUnixSupport(NativeFunction.SYSTEM_MEMORY);
        return nativeUnixSupport.getSystemMemory();
    }

    /**
     * Get the effective User ID on a Unix System
     *
     * @return
     * @throws hudson.util.jna.Native.ExecutionError
     */
    public int getEuid() throws NativeAccessException {
        ensureUnixSupport(NativeFunction.EUID);
        return nativeUnixSupport.getEuid();
    }

    /**
     * Get the effective Group ID on a Unix System
     *
     * @return
     * @throws hudson.util.jna.Native.ExecutionError
     */
    public int getEgid() throws NativeAccessException {
        ensureUnixSupport(NativeFunction.EGID);
        return nativeUnixSupport.getEgid();
    }

    /**
     * Check if this Unix user exists on the machine where this program runs
     *
     * @param userName
     * @return
     * @throws hudson.util.jna.Native.NativeExecutionException
     */
    public boolean checkUnixUser(String userName) throws NativeAccessException {
        ensureUnixSupport(NativeFunction.UNIX_USER);
        return nativeUnixSupport.checkUnixUser(userName);
    }

    /**
     * Check if this Unix group exists on the machine where this program runs
     *
     * @param userName
     * @return
     * @throws hudson.util.jna.Native.NativeExecutionException
     */
    public boolean checkUnixGroup(String groupName) throws NativeAccessException {
        ensureUnixSupport(NativeFunction.UNIX_GROUP);
        return nativeUnixSupport.checkUnixGroup(groupName);
    }

    /**
     * Authenticate using Using Unix Pluggable Authentication Modules (PAM)
     *
     * @param userName
     * @param password
     * @return List<String> list of groups to which this user belongs
     * @throws hudson.util.jna.Native.NativeExecutionException
     */
    public Set<String> pamAuthenticate(String serviceName, String userName, String password) throws NativeAccessException {
        ensureUnixSupport(NativeFunction.PAM);
        return nativeUnixSupport.pamAuthenticate(serviceName, userName, password);
    }

    /**
     * Check if PAM Authentication available in the machine where this program
     * runs
     *
     * @return Message corresponding to the availability of PAM
     * @throws hudson.util.jna.Native.NativeExecutionException
     */
    public String checkPamAuthentication() throws NativeAccessException {
        ensureUnixSupport(NativeFunction.PAM);
        return nativeUnixSupport.checkPamAuthentication();
    }

    /**
     * Restart current Java process (JVM in which this application is running)
     *
     * @throws hudson.util.jna.Native.NativeExecutionException
     */
    public void restartJavaProcess(Map<String, String> properties, boolean asDaemon) throws NativeAccessException {
        ensureUnixSupport(NativeFunction.JAVA_RESTART);
        nativeUnixSupport.restartJavaProcess(properties, asDaemon);
    }

    /**
     * Check if this Java process can be restarted
     *
     * @throws hudson.util.jna.Native.NativeExecutionException
     */
    public boolean canRestartJavaProcess() throws NativeAccessException {
        ensureUnixSupport(NativeFunction.JAVA_RESTART);
        return nativeUnixSupport.canRestartJavaProcess();
    }

    /**
     * Get the error associated with the last Native Unix Operation
     *
     * @return String error message
     */
    public String getLastUnixError() {
        if (!hasUnixSupport()) {
            return "Native Unix Support plugin not installed";
        }
        return nativeUnixSupport.getLastError();
    }

    /**
     * Check if .NET is installed on a the Windows machine
     *
     * @return true if .NET is installed.
     * @throws hudson.util.jna.Native.ExecutionError
     */
    public boolean isDotNetInstalled(int major, int minor) throws NativeAccessException {
        ensureWindowsSupport(NativeFunction.DOTNET);
        return nativeWindowsSupport.isDotNetInstalled(major, minor);
    }

    /**
     * Get all the native processes on a Windows System
     *
     * @return List of Native Window Processes
     * @throws hudson.util.jna.Native.ExecutionError
     */
    public List<NativeProcess> getWindowsProcesses() throws NativeAccessException {
        ensureWindowsSupport(NativeFunction.WINDOWS_PROCESS);
        return nativeWindowsSupport.getWindowsProcesses();
    }

    /**
     * Find the Native Process Id of the given java.lang.process
     *
     * @param process (java.lang.process)
     * @return pid, the Native Process ID
     */
    public int getWindowsProcessId(Process process) throws NativeAccessException {
        ensureWindowsSupport(NativeFunction.WINDOWS_PROCESS);
        return nativeWindowsSupport.getWindowsProcessId(process);
    }

    /**
     * Run the Windows program natively in an elevated privilege
     *
     * @param winExe, windows executable to run
     * @param args, arguments to pass
     * @param logFile, File where the logs of the process should go
     * @param pwd, Path of the working directory
     * @return int, process exit code
     * @throws hudson.util.jna.Native.NativeExecutionException
     */
    public int windowsExec(File winExe, String args, String logFile, File pwd) throws NativeAccessException {
        ensureWindowsSupport(NativeFunction.WINDOWS_EXEC);
        return nativeWindowsSupport.windowsExec(winExe, args, logFile, pwd);
    }

    /**
     * Move a Windows File using native win32 library
     *
     * @param fromFile
     * @param toFile
     * @return
     * @throws hudson.util.jna.Native.NativeExecutionException
     */
    public void windowsMoveFile(File fromFile, File toFile) throws NativeAccessException {
        ensureWindowsSupport(NativeFunction.WINDOWS_FILE_MOVE);
        nativeWindowsSupport.windowsMoveFile(fromFile, toFile);
    }

    /**
     * Get the error associated with the last Native Unix Operation
     *
     * @return String error message
     */
    public String getLastWindowsError() {
        if (!hasUnixSupport()) {
            return "Native Windows Support plugin not installed";
        }
        return nativeWindowsSupport.getLastError();
    }

    /**
     * Get the Native processes of a Mac System
     *
     * @return
     * @throws hudson.util.jna.Native.NativeExecutionException
     */
    public List<NativeProcess> getMacProcesses() throws NativeAccessException {
        ensureMacSupport(NativeFunction.MAC_PROCESS);
        return nativeMacSupport.getMacProcesses();
    }

    /**
     * Get the error associated with the last Native Unix Operation
     *
     * @return String error message
     */
    public String getLastMacError() {
        return nativeMacSupport.getLastError();
    }

    /**
     * Fetch the list of mounted ZFS roots
     *
     * @return
     * @throws hudson.util.jna.Native.NativeExecutionException
     */
    public List<NativeZfsFileSystem> getZfsRoots() throws NativeAccessException {
        ensureZfsSupport(NativeFunction.ZFS);
        return nativeZfsSupport.getZfsRoots();
    }

    /**
     * Find the ZFS File System by its mount point
     *
     * @return
     * @throws hudson.util.jna.Native.NativeExecutionException
     */
    public NativeZfsFileSystem getZfsByMountPoint(File mountPoint) throws NativeAccessException {
        ensureZfsSupport(NativeFunction.ZFS);
        return nativeZfsSupport.getZfsByMountPoint(mountPoint);
    }

    /**
     * Create ZFS File System corresponding to the mount name
     *
     * @param mountPoint
     * @return ZFS File System if created successfully
     * @throws hudson.util.jna.Native.NativeExecutionException
     */
    public NativeZfsFileSystem createZfs(String mountName) throws NativeAccessException {
        ensureZfsSupport(NativeFunction.ZFS);
        return nativeZfsSupport.createZfs(mountName);
    }

    /**
     * Open the target ZFS File System
     *
     * @param mountPoint
     * @return ZFS File System if opened successfully
     * @throws hudson.util.jna.Native.NativeExecutionException
     */
    public NativeZfsFileSystem openZfs(String target) throws NativeAccessException {
        ensureZfsSupport(NativeFunction.ZFS);
        return nativeZfsSupport.openZfs(target);
    }

    /**
     * Check if the named ZFS exists
     *
     * @param zfsName
     * @return
     * @throws hudson.util.jna.Native.NativeExecutionException
     */
    public boolean zfsExists(String zfsName) throws NativeAccessException {
        ensureZfsSupport(NativeFunction.ZFS);
        return nativeZfsSupport.zfsExists(zfsName);
    }

    /**
     * Get the error associated with the last Native Unix Operation
     *
     * @return String error message
     */
    public String getLastZfsError() {
        if (!hasZfsSupport()) {
            return "Native Unix Support plugin not installed";
        }
        return nativeZfsSupport.getLastError();
    }
}
