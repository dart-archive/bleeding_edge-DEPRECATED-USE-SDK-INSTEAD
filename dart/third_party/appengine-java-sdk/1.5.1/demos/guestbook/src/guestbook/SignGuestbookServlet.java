package guestbook;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;
import javax.jdo.PersistenceManager;
import javax.servlet.http.*;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import guestbook.Greeting;
import guestbook.PMF;

public class SignGuestbookServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(SignGuestbookServlet.class.getName());

    public void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();

        String content = req.getParameter("content");
        Date date = new Date();
        Greeting greeting = new Greeting(user, content, date);

        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            pm.makePersistent(greeting);
        } finally {
            pm.close();
        }

        resp.sendRedirect("/guestbook.jsp");
    }
}
