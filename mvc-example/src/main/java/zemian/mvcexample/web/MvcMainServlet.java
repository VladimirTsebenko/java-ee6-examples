package zemian.mvcexample.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import zemian.mvcexample.service.Application;
import zemian.mvcexample.web.controller.Controller;
import zemian.mvcexample.web.controller.HelloController;
import zemian.mvcexample.web.controller.IndexController;
import zemian.mvcexample.web.controller.SysPropsController;
import zemian.mvcexample.web.controller.WebRequest;
import zemian.service.logging.Logger;
import zemian.service.util.Tuple;
import zemian.service.util.Utils;

/**
 * A main controller dispatcher that parse http request and delegate to user
 * controller handlers. User should register their controllers into the Application
 * space.
 * 
 * @author zedeng
 */
@WebServlet("/main/*")
public class MvcMainServlet extends HttpServlet {
    private static final Logger LOGGER = new Logger(MvcMainServlet.class);
    private static final long serialVersionUID = 1L;
    private static final String TEMPLATE_PATH = "/jsp";
    
    @Override
    public void init() throws ServletException {
        registerControllers();
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Tuple<String, String> paths = parsePaths(req);
        String controllerPath = paths.getA();
        String viewPath = paths.getB();
        LOGGER.debug("Processing controllerPath=%s, viewPath=%s", controllerPath, viewPath);
        Map<String, Controller> controllers = Application.getInstance().getControllers();
        Controller controller = controllers.get(controllerPath);
        if (controller == null)
            throw new RuntimeException("No controller mapped for " + controllerPath);
        
        WebRequest webRequest = new WebRequest();
        webRequest.setHttpServletRequest(req);
        webRequest.setMainServlet(this);
        
        Object model = controller.handle(webRequest);
        addModelToRequestAttributes(model, req);
        req.getRequestDispatcher(paths.getB()).forward(req, resp);
    }
    
    protected Tuple<String, String> parsePaths(HttpServletRequest req) {
        // Example of uri: /mvc-example/
        //                 /mvc-example/mycontroller

        String uri = req.getRequestURI();
        LOGGER.trace("Parsing uri=%s", uri);
        if (uri.endsWith("/")) 
            uri = uri.substring(0, uri.length() - 1);
        String[] paths = uri.split("/"); // We will have min of two elements
        //String emptyPath = paths[0];
        //String contextPath = paths[1];
        String controllerPath = "index";
        String resourcePath = "";
        if (paths.length >= 3) {
            // This case is when servlet name is mapped, so we need to parse that out.
            //String servletPath = paths[2];
            if (paths.length >= 4) {
                controllerPath = paths[3];
                resourcePath = Utils.join("/", Arrays.copyOfRange(paths, 4, paths.length));
            }
        }
        String viewPath = TEMPLATE_PATH + resourcePath + "/" + controllerPath + ".jsp";
        return Tuple.tuple(controllerPath, viewPath);
    }

    protected void addModelToRequestAttributes(Object model, HttpServletRequest req) {
        if (model instanceof Map) {
            Map<?, ?> mapModel = (Map<?,?>)model;
            for (Map.Entry<?, ?> entry : mapModel.entrySet()) {
                req.setAttribute(entry.getKey().toString(), entry.getValue());
            }
        } else {
            req.setAttribute("model", model);
        }
    }

    protected void registerControllers() {
        // TODO: We need to make these controllers configurable outside of this controller.
        Map<String, Controller> controllers = Application.getInstance().getControllers();
        controllers.put("index", new IndexController());
        controllers.put("hello", new HelloController());
        controllers.put("sys-props", new SysPropsController());
    }
}
