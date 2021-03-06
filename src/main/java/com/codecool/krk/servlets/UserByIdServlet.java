import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import connections.SingletonEntityManagerFactory;
import models.User;
import servletHelpers.ServletHelper;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@WebServlet(urlPatterns = {"/user/*"})
public class UserByIdServlet extends HttpServlet {
    private EntityManagerFactory emf = SingletonEntityManagerFactory.getInstance();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)  {
        String url = request.getRequestURI();
        int userId = Integer.parseInt(url.replace("/user/", ""));
        String json = getUserJsonById(userId, request, response);
        response.setHeader("Content-type", "application/json");
        try{
            response.getWriter().print(json);
        }catch(IOException ioexc) {
            System.out.println("Something's wrong with the input!");
        }

    }


    protected void doPost(HttpServletRequest request, HttpServletResponse response){
        EntityManager entityManager = emf.createEntityManager();
        ServletHelper servletHelper = new ServletHelper();
        String json;
        try{
            json = servletHelper.parseRequest(request);
        }catch(IOException ioexc) {
            System.out.println("Buffered reader exception!");
            return;
        }
        Gson gson = new Gson();
        User user = gson.fromJson(json, User.class);
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        entityManager.persist(user);
        transaction.commit();
        entityManager.close();
    }

    protected void doPut(HttpServletRequest request, HttpServletResponse response){
        EntityManager entityManager = emf.createEntityManager();
        ServletHelper servletHelper = new ServletHelper();
        String json;
        try{
             json = servletHelper.parseRequest(request);
        }catch(IOException exc) {
            System.out.println("Buffered reader in servlet helper exception!");
            return;
        }
        User userFromRequest = getUserByJson(json);
        String url = request.getRequestURI();
        int userId = Integer.parseInt(url.replace("/user/", ""));
        User oldUser = entityManager.find(User.class, userId);
        if(oldUser == null) {
            postUserWhenNull(response, entityManager, userFromRequest);
        }else {
            userFromRequest.setId(userId);
            updateUser(response, entityManager, userFromRequest);
        }



    }


    protected void doDelete(HttpServletRequest request, HttpServletResponse response) {
        EntityManager entityManager = emf.createEntityManager();
        String url = request.getRequestURI();
        int userId = Integer.parseInt(url.replace("/user/", ""));
        User user = entityManager.find(User.class, userId);
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        entityManager.remove(user);
        transaction.commit();
    }


    private String getUserJsonById(int id, HttpServletRequest request,  HttpServletResponse response)  {
        EntityManager entityManager = emf.createEntityManager();
        User user = entityManager.find(User.class, id);
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        user.setNotesIds();
        return gson.toJson(user);
    }




    private void updateUser(HttpServletResponse response, EntityManager entityManager, User user)  {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        entityManager.merge(user);
        transaction.commit();
        entityManager.close();
        try{
            response.getWriter().print("{edit successful}");
        }catch(IOException ioexc) {
            System.out.println("Printing json to client exception!");
        }

    }


    private void postUserWhenNull(HttpServletResponse response, EntityManager entityManager, User user) { //uzyc response'a
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        entityManager.persist(user);
        transaction.commit();
        entityManager.close();
    }

    private User getUserByJson(String requestJSON) {
        boolean exist = requestJSON.contains("\"id\"");
        if (exist) {
            String[] arr = requestJSON.split(",",2);
            requestJSON = arr[1];
            requestJSON = "{" + requestJSON;
            System.out.println(requestJSON);
        }
        Gson gson = new Gson();
        User user = gson.fromJson(requestJSON, User.class);
        return user;
    }
}
