package com.nitorcreations;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.nitorcreations.PresentationHttpServer.Range;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
class RequestHandler implements HttpHandler {
	private final String context;
	private PresentationController controller = null;
	
	public RequestHandler(String context) {
		this.context = context;
	}
	
	public RequestHandler(String context, PresentationController controller) {
		this.context = context;
		this.controller = controller;
	}
	
	public void handle(HttpExchange exchange) throws IOException {
		String requestMethod = exchange.getRequestMethod();
		if (requestMethod.equalsIgnoreCase("GET")) {
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Accept-Ranges", "bytes");

			URI uri = exchange.getRequestURI();
			String path = uri.getPath().substring(context.length() + 1);
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			
			if (path.startsWith("show") && context.equals("run")) {
				responseHeaders.set("Content-Type", "text/plain");
				try {
					int slideIndex = Integer.parseInt(path.split("/")[1]);
					if (path.startsWith("showquick")) {
						controller.showSlide(slideIndex, true);
					} else {
						controller.showSlide(slideIndex);
					}
					exchange.sendResponseHeaders(200, 0);
					OutputStream responseBody = exchange.getResponseBody();
					responseBody.write(Integer.toString(controller.curentSlide()).getBytes());
					responseBody.close();
				} catch (NullPointerException | ArrayIndexOutOfBoundsException | NumberFormatException e) {
					System.out.println("Illegal slide show command " + path);
					exchange.sendResponseHeaders(400, 0);
					OutputStream responseBody = exchange.getResponseBody();
					responseBody.write(("Bad request: " + path).getBytes());
					responseBody.close();
				}
				return;
			}

			if (path.startsWith("currentslide") && (context.equals("run") || context.equals("follow"))) {
				responseHeaders.set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, 0);
				OutputStream responseBody = exchange.getResponseBody();
				responseBody.write(Integer.toString(controller.curentSlide()).getBytes());
				responseBody.close();
				return;
			}

			if (path.startsWith("slidecount") && (context.equals("run") || context.equals("follow"))) {
				responseHeaders.set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, 0);
				OutputStream responseBody = exchange.getResponseBody();
				responseBody.write(Integer.toString(controller.curentSlide()).getBytes());
				responseBody.close();
				return;
			}
			
			
			String resourceName = "html/" + path; 
			if ("html/".equals(resourceName) || resourceName.startsWith("html/index")) {
				if (context.length() == 0) {
					resourceName = "html/index-default.html";
				} else {
					resourceName = "html/index-" + context + ".html";
				}
			}
			List<String> inmatch = exchange.getRequestHeaders().get("If-None-Match");
			List<String> range = exchange.getRequestHeaders().get("Range");
			if (inmatch != null && inmatch.size() > 0 && inmatch.get(0).equals(Utils.md5sums.get(resourceName)) && 
					(range == null || range.size() == 0)) {
				responseHeaders.set("Accept-Ranges", "bytes");
				responseHeaders.set("ETag", inmatch.get(0));
				exchange.sendResponseHeaders(304, -1);
				return;
			}
			byte[] content = Utils.getContent(resourceName);
			responseHeaders.set("ETag", Utils.md5sums.get(resourceName));
			if (content != null) {
				if (uri.getPath().endsWith(".html")) {
					responseHeaders.set("Content-Type", "text/html");
				} else if (uri.getPath().toLowerCase().endsWith(".png")) {
					responseHeaders.set("Content-Type", "image/png");
				} else if (uri.getPath().toLowerCase().endsWith(".jpg") ||
						uri.getPath().toLowerCase().endsWith(".jpeg")) {
					responseHeaders.set("Content-Type", "image/jpeg");
				} else if (uri.getPath().toLowerCase().endsWith(".mp4")) {
					responseHeaders.set("Content-Type", "video/mp4");
				} else if (uri.getPath().toLowerCase().endsWith(".ogv")) {
					responseHeaders.set("Content-Type", "video/ogg");
				}
				if (range == null || range.size() == 0) {
					exchange.sendResponseHeaders(200, content.length);
					OutputStream responseBody = exchange.getResponseBody();
					responseBody.write(content);
					responseBody.close();
					return;
				} else {
					List<Range> ranges = new ArrayList<>();
					for (String nextRange : range) {
						Range currentRange = new PresentationHttpServer.Range();
						currentRange.length = content.length;
						if (nextRange.startsWith("bytes=")) {
							nextRange = nextRange.substring(6);
						}
						int dashPos = nextRange.indexOf('-');
						if (dashPos == 0) {
							try {
								int offset = Integer.parseInt(nextRange);
								currentRange.start = content.length - offset;
								currentRange.end = content.length  - 1;
							} catch (NumberFormatException e) {
								responseHeaders.set("Content-Range", "bytes */" + content.length);
								exchange.sendResponseHeaders(416,  -1);
								return;
							}
						} else {
							try {
								currentRange.start = Integer.parseInt(nextRange.substring(0, dashPos));
								if (dashPos < nextRange.length() - 1) {
									currentRange.end = Integer.parseInt(nextRange.substring
											(dashPos + 1, nextRange.length()));
								} else {
									currentRange.end = content.length-1;
								}
							} catch (NumberFormatException e) {
								responseHeaders.set("Content-Range", "bytes */" + content.length);
								exchange.sendResponseHeaders(416,  -1);
								return;
							}
						}
						if (!currentRange.validate()) {
							responseHeaders.set("Content-Range", "bytes */" + content.length);
							exchange.sendResponseHeaders(416,  -1);
							return;
						}
						ranges.add(currentRange);
					}
					Range ret = ranges.get(0);
					responseHeaders.set("Content-Range", "bytes " + ret.start
							+ "-" + ret.end + "/"
							+ ret.length);
					exchange.sendResponseHeaders(206, ret.rangeLen);
					OutputStream responseBody = exchange.getResponseBody();
					responseBody.write(content, ret.start, ret.rangeLen);
					responseBody.close();
				}
			} else {
				responseHeaders.set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(404, 0);
				OutputStream responseBody = exchange.getResponseBody();
				responseBody.write("Not found".getBytes());
				responseBody.close();

			}
		}
	}
}
