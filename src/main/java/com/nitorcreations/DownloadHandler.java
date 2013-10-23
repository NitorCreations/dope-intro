package com.nitorcreations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
class DownloadHandler implements HttpHandler {

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		Headers responseHeaders = exchange.getResponseHeaders();
		String path = exchange.getRequestURI().getPath().substring("/download".length());
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		if ((path.startsWith("slides") || path.startsWith("html")) && path.endsWith(".zip")) {
			String slideSet = path.substring(0, path.length() - 4);
			String slideDir = slideSet + "/";
			String[] slides = Utils.getResourceListing(slideDir);
			if (slides == null || slides.length == 0) {
				responseHeaders.set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(404, 0);
				OutputStream responseBody = exchange.getResponseBody();
				responseBody.write("Not found".getBytes());
				responseBody.close();
				return;
			}
			responseHeaders.set("Content-Type", "application/zip");
			responseHeaders.set("Content-disposition", "attachment; filename=" + path);
			exchange.sendResponseHeaders(200, 0);
			ZipOutputStream out = new ZipOutputStream(exchange.getResponseBody());
			List<String> slideNames = Arrays.asList(slides);
			Collections.sort(slideNames);
			for (String next : slideNames) {
				if (next.isEmpty() || next.equals("/")) continue;
				if (next.endsWith(".video")) {
					try (BufferedReader in = 
							new BufferedReader(new InputStreamReader(Utils.getResource(slideDir + next)))) {
						String video=in.readLine();
						writeNextEntry(out, Utils.getResource("html/" + video), next + "." + video);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					writeNextEntry(out, Utils.getResource(slideDir + next), next);
				}	
			}
			out.close();
		} else if (path.equals("presentation-small-images.zip")) {
			responseHeaders.set("Content-Type", "application/zip");
			responseHeaders.set("Content-disposition", "attachment; filename=presentation-images.zip");
		} else if (path.equals("presentation-small-images.zip")) {
			responseHeaders.set("Content-Type", "application/zip");
			responseHeaders.set("Content-disposition", "attachment; filename=presentation-images.zip");
		} else {
			responseHeaders.set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(404, 0);
			OutputStream responseBody = exchange.getResponseBody();
			responseBody.write("Not found".getBytes());
			responseBody.close();
		}
	}
	
	final byte[] buffer = new byte[1024];
	
	private void writeNextEntry(ZipOutputStream out, InputStream in, String name) {
        try {
        	out.putNextEntry(new ZipEntry(name));
        	int count;

        	while ((count = in.read(buffer)) > 0) {
        		out.write(buffer, 0, count);
        	}
        } catch (IOException e) {
        	e.printStackTrace();
        } finally {
        	if (in != null) {
        		try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        }
	}
}
