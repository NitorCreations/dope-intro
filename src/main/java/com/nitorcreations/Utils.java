package com.nitorcreations;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Utils {
	private static Map<String, byte[]> contents = new HashMap<>();
	public  static Map<String, String> md5sums = new ConcurrentHashMap<>();
	private static MessageDigest md5;
	static {
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

    static final byte[] HEX_BYTES = new byte[]
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

	public static String[] getResourceListing(String path) {
		URL dirURL = Utils.class.getClassLoader().getResource(path);
		if (dirURL == null) {
			String me = Utils.class.getName().replace(".", "/")+".class";
			dirURL = Utils.class.getClassLoader().getResource(me);
		}
		if (dirURL != null) {
			if  (dirURL.getProtocol().equals("file")) {
				try {
					File[] files = new File(dirURL.toURI()).listFiles();
					String[] ret = new String[files.length];
					for (int i = 0; i < files.length; i++) {
						if (files[i].isDirectory()) {
							ret[i] = files[i].getName() + "/";
						} else {
							ret[i] = files[i].getName();
						}
					}
					return ret;
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			} else if (dirURL.getProtocol().equals("jar")) {
				String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!"));
				Set<String> entries = Utils.getJarEntries(jarPath); 
				Set<String> result = new HashSet<String>(); 
				for (String name : entries) {
					if (name.startsWith(path)) {
						String entry = name.substring(path.length());
						int checkSubdir = entry.indexOf("/");
						if (checkSubdir >= 0) {
							entry = entry.substring(0, checkSubdir + 1);
						}
						result.add(entry);
					}
				}
				return result.toArray(new String[result.size()]);
			} 
		}
		throw new UnsupportedOperationException("Cannot list files for URL "+dirURL);
	}

	public static synchronized Set<String> getJarEntries(String jarPath) {
		Set<String> result = new HashSet<String>();
		JarFile jar = null;
		try {
			File jarFile = new File(URLDecoder.decode(jarPath, "UTF-8"));
			if (BumpAndFadeController.jarEntryCache.containsKey(jarFile.getAbsolutePath())) {
				return BumpAndFadeController.jarEntryCache.get(jarFile.getAbsolutePath());
			} else {
				jar = new JarFile(jarFile);
				Enumeration<JarEntry> entries = jar.entries(); 
				while(entries.hasMoreElements()) {
					result.add(entries.nextElement().getName());
				}
				BumpAndFadeController.jarEntryCache.put(jarFile.getAbsolutePath(), result);
			}
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	public static void runVideo(final File video) {
		try {
			Process p = Runtime.getRuntime().exec("videoplayer " + video.getAbsolutePath());

			final BufferedReader stdInput = new BufferedReader(new 
					InputStreamReader(p.getInputStream()));

			final BufferedReader stdError = new BufferedReader(new 
					InputStreamReader(p.getErrorStream()));

			new Thread(new Runnable() {
				String s = null;
				public void run() {
					try {
						while ((s = stdInput.readLine()) != null) {
							System.out.println(s);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				};
			}).start();

			new Thread(new Runnable() {
				String s = null;
				public void run() {
					try {
						while ((s = stdError.readLine()) != null) {
							System.out.println(s);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
			p.waitFor();
		}
		catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public static InputStream getResource(String name) {
		System.out.println("Getting resource: " + name);
		return Utils.class.getClassLoader().getResourceAsStream(name);
	}

	public static String toHexString(byte[] digest) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < digest.length; ++i) {
			sb.append(Integer.toHexString((digest[i] & 0xFF) | 0x100).substring(1,3));
		}
		return sb.toString();
	}

	public static byte[] toHexBytes(byte[] toBeConverted) {
	    if (toBeConverted == null) {
	        throw new NullPointerException("Parameter to be converted can not be null");
	    }
	
	    byte[] converted = new byte[toBeConverted.length * 2];
	    for (int i = 0; i < toBeConverted.length; i++) {
	        byte b = toBeConverted[i];
	        converted[i * 2] = HEX_BYTES[b >> 4 & 0x0F];
	        converted[i * 2 + 1] = HEX_BYTES[b & 0x0F];
	    }
	
	    return converted;
	}

	synchronized static byte[] getContent(String resourceName) throws IOException {
		byte[] cached = contents.get(resourceName);
		if (cached == null) {
			try (InputStream in = Utils.getResource(resourceName)) {
				if (in == null) {
					return null;
				}
				byte[] buffer = new byte[1024];
				int len = in.read(buffer);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				while (len != -1) {
					out.write(buffer, 0, len);
					len = in.read(buffer);
				}
				cached = out.toByteArray();
				md5.reset();
				byte[] digest = md5.digest(cached);
				md5sums.put(resourceName, Utils.toHexString(digest));
				if (System.getProperty("nocache") == null) {
					contents.put(resourceName, cached);
				}
			}
		}
		return cached;
	}

}
