package com.google.sps;

import java.net.URL;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.KeyFactory;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

/**
 * Starts up the server, including a DefaultServlet that handles static files, and any servlet
 * classes annotated with the @WebServlet annotation.
 */
public class ServerMain {

  public static void main(String[] args) throws Exception {

    // Create a server that listens on port 8080.
    Server server = new Server(8080);
    WebAppContext webAppContext = new WebAppContext();
    server.setHandler(webAppContext);

    // Load static content from inside the jar file.
    URL webAppDir = ServerMain.class.getClassLoader().getResource("META-INF/resources");
    webAppContext.setResourceBase(webAppDir.toURI().toString());

    // Enable annotations so the server sees classes annotated with @WebServlet.
    webAppContext.setConfigurations(
        new Configuration[] {
          new AnnotationConfiguration(), new WebInfConfiguration(),
        });

    // Look for annotations in the classes directory (dev server) and in the jar file (live server)
    webAppContext.setAttribute(
        "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
        ".*/target/classes/|.*\\.jar");

    // Handle static resources, e.g. html files.
    ServletHolder defaultServletHolder = webAppContext.addServlet(DefaultServlet.class, "/");
    defaultServletHolder.setInitParameter("cacheControl", "no-store, max-age=0");

    // Start the server! 🚀
    server.start();
    System.out.println("Server started!");

    // Keep the main thread alive while the server is running.
    server.join();
  }
}

/** Servlet responsible for creating new tasks. */
@WebServlet("/new-task")
public class NewTaskServlet extends HttpServlet {
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Sanitize user input to remove HTML tags and JavaScript.
    String title = Jsoup.clean(request.getParameter("title"), Whitelist.none());
    long timestamp = System.currentTimeMillis();

    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    KeyFactory keyFactory = datastore.newKeyFactory().setKind("Task");
    FullEntity taskEntity =
        Entity.newBuilder(keyFactory.newKey())
            .set("title", title)
            .set("timestamp", timestamp)
            .build();
    datastore.put(taskEntity);



    response.sendRedirect("/index.html");
  }
}
