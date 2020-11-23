// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.environment.webserver;

import com.google.common.collect.ImmutableMap;
import org.openqa.selenium.grid.config.MapConfig;
import org.openqa.selenium.grid.config.MemoizedConfig;
import org.openqa.selenium.grid.server.BaseServerOptions;
import org.openqa.selenium.grid.server.Server;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.io.TemporaryFilesystem;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.netty.server.NettyServer;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpHandler;
import org.openqa.selenium.remote.http.HttpMethod;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.openqa.selenium.remote.http.Contents.bytes;
import static org.openqa.selenium.remote.http.Contents.string;

public class NettyAppServer implements AppServer {

  private final Server<?> server;

  public NettyAppServer() {
    MemoizedConfig config = new MemoizedConfig(new MapConfig(singletonMap("server", singletonMap("port", PortProber.findFreePort()))));
    BaseServerOptions options = new BaseServerOptions(config);

    File tempDir = TemporaryFilesystem.getDefaultTmpFS().createTempDir("generated", "pages");

    HttpHandler handler = new HandlersForTests(
      options.getHostname().orElse("localhost"),
      options.getPort(),
      tempDir.toPath());

    server = new NettyServer(options, handler);
  }

  public NettyAppServer(HttpHandler handler) {
    Require.nonNull("Handler", handler);

    int port = PortProber.findFreePort();
    server = new NettyServer(
      new BaseServerOptions(new MapConfig(singletonMap("server", singletonMap("port", port)))),
      handler);
  }

  @Override
  public void start() {
    server.start();
  }

  @Override
  public void stop() {
    server.stop();
  }

  @Override
  public String whereIs(String relativeUrl) {
    return createUrl("http", getHostName(), relativeUrl);
  }

  @Override
  public String whereElseIs(String relativeUrl) {
    return createUrl("http", getAlternateHostName(), relativeUrl);
  }

  @Override
  public String whereIsSecure(String relativeUrl) {
    return createUrl("https", getHostName(), relativeUrl);
  }

  @Override
  public String whereIsWithCredentials(String relativeUrl, String user, String password) {
    return String.format
        ("http://%s:%s@%s:%d/%s",
         user,
         password,
         getHostName(),
         server.getUrl().getPort(),
         relativeUrl);
  }

  private String createUrl(String protocol, String hostName, String relativeUrl) {
    if (!relativeUrl.startsWith("/")) {
      relativeUrl = "/" + relativeUrl;
    }

    try {
      return new URL(
          protocol,
          hostName,
          server.getUrl().getPort(),
          relativeUrl)
          .toString();
    } catch (MalformedURLException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public String create(Page page) {
    try {
      byte[] data = new Json()
          .toJson(ImmutableMap.of("content", page.toString()))
          .getBytes(UTF_8);

      HttpClient client = HttpClient.Factory.createDefault().createClient(new URL(whereIs("/")));
      HttpRequest request = new HttpRequest(HttpMethod.POST, "/common/createPage");
      request.setHeader(CONTENT_TYPE, JSON_UTF_8.toString());
      request.setContent(bytes(data));
      HttpResponse response = client.execute(request);
      return string(response);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public String getHostName() {
    return AppServer.detectHostname();
  }

  @Override
  public String getAlternateHostName() {
    return AppServer.detectAlternateHostname();
  }

  public static void main(String[] args) {
    NettyAppServer server = new NettyAppServer();
    server.start();

    System.out.println(server.whereIs("/"));
  }
}
