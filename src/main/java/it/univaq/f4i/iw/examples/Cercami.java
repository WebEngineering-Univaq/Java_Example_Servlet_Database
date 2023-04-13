/*
 * ATTENZIONE: il codice di questa classe dipende dalla corretta definizione delle
 * risorse presente nei file context.xml (Resource) e web.xml (resource-ref)
 * 
 * ATTENZIONE: in codice fa uso del driver per MySQL versione 8. Se questo driver
 * è già presente nel vostro server, non sarà necessario aggiungerlo come libreria
 * al progetto (creandone una copia "privata"). Tuttavia, se il server disponesse solo
 * della versione 5 del driver (ancora molto comune), ricevereste un errore del tipo
 * "impossibile trovare la classe com.mysql.cj.jdbc.Driver", e in tal caso dovreste
 * inserire il jar del connector/J 8 nella vostra applicazione
 * 
 * ATTENZIONE: il codice fa uso di un database configurato come segue:
 * - database 'webdb2' su DBMS MySQL in esecuzione su localhost
 * - utente 'website' con password 'webpass' autorizzato nel DBMS 
 *   a leggere i dati del suddetto database
 * - tabella 'author' presente nel suddetto database, con almeno le 
 *   colonne 'name' e 'surname' di tipo stringa

 * WARNING: this class needs the definition of an external data source to work correctly.
 * See the Resource element in context.xml and the resource-ref element in web.xml
 *
 * WARNING: this class uses the MySQL driver version 8. If this driver is already present 
 * on your server, it will not be necessary to add it as a library to the project 
 * (creating a "private" copy). However, if the server has version 5 of the driver pre-installed
 * (still very common), you will receive an error like "class com.mysql.cj.jdbc.Driver not found", 
 * and in this case you should add the connector/J 8 jar in your application libraries.
 * 
 * WARNING: the code makes use of a database configured as follows:
 * - 'webdb2' database on a MySQL DBMS running on localhost
 * - user 'website' with password 'webpass' authorized in the DBMS to read the 
 *   data of the aforementioned database
 * - 'author' table present in the aforementioned database, 
 *   with at least the string-typed 'name' and 'surname' columns
 */
package it.univaq.f4i.iw.examples;

import it.univaq.f4i.iw.framework.result.HTMLResult;
import it.univaq.f4i.iw.framework.utils.ServletHelpers;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 *
 * @author Giuseppe Della Penna
 */
public class Cercami extends HttpServlet {

    ///iniettiamo un riferimento alla DataSource che gestisce il pool di connessioni (per la versione pooling_global)
    //inject a reference to the DataSource object (for the pooling_global version)
    @Resource(name = "jdbc/webdb2")
    private DataSource dsg;

    //e' sempre opportuno separare il testo SQL da codice
    //it is always better keep the SQL code distinct from the Java cone
    private static final String SQL_SELECT_AUTHOR = "SELECT * FROM author WHERE name LIKE ?";

    private void print_author_results(String name, ResultSet rs, HTMLResult result) throws SQLException {
        result.appendToBody("<p><b>Query:</b> " + HTMLResult.sanitizeHTMLOutput(name) + "</p>");
        result.appendToBody("<table border = '1'>");
        result.appendToBody("<tr><th>ID</th><th>Name</th><th>Surname</th></tr>");
        //iteriamo sulle righe del risultato
        //iterate on the result rows
        while (rs.next()) {
            result.appendToBody("<tr>");
            //preleviamo le colonne usando i loro nomi
            //get the column values using their name
            result.appendToBody("<td>" + rs.getInt("ID")
                    + "</td><td>" + HTMLResult.sanitizeHTMLOutput(rs.getString("name"))
                    + "</td><td>" + HTMLResult.sanitizeHTMLOutput(rs.getString("surname")) + "</td>");
            result.appendToBody("</tr>");
        }
        result.appendToBody("</table>");
        result.appendToBody("<p><a href=\"cercami\">Try again!</a></p>");

    }

    private void action_query(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException, ClassNotFoundException, NamingException {
        String mode = request.getParameter("mode");
        switch (mode) {
            case "psql":
                action_query_JDBC_SQL_Precompiled(request, response);
            case "pool":
                action_query_Pool(request, response);
            case "gpool":
                action_query_Pool_Injection(request, response);
            default:
                action_query_JDBC_SQL_Direct(request, response);
        }
    }

    //ATTENZIONE: NON usate questo metodo di accesso al db nelle web application!
    //WARNING: DO NOT use this db access method in web applications!
    private void action_query_JDBC_SQL_Direct(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException, ClassNotFoundException {
        String name = request.getParameter("n");

        HTMLResult result = new HTMLResult(getServletContext());
        result.setTitle("Search results (DIRECT CONNECTION, DIRECT SQL)");

        //caricamento dinamico della classe driver. 
        //usiamo dei parametri del contesto per configurarlo (vedi web.xml)            
        //dynamic loading of the driver class
        //from the context initialization parameters (see web.xml)
        Class.forName(getServletContext().getInitParameter("data.jdbc.driver"));

        //usiamo il try-with-resources per ottimizzare il processo di chiusura delle risorse
        try (
                //connessione al database locale
                //database connection
                Connection connection = DriverManager.getConnection(getServletContext().getInitParameter("data.jdbc.connectionstring"), getServletContext().getInitParameter("data.jdbc.username"), getServletContext().getInitParameter("data.jdbc.password"));
                //eseguiamo la query (SQL diretto)
                //query execution (direct SQL)
                Statement s = connection.createStatement();
                ResultSet rs = s.executeQuery("SELECT * FROM author WHERE name LIKE '%" + name + "%'")) {

            print_author_results(name, rs, result);
            result.activate(request, response);

        }
    }

    //ATTENZIONE: NON usate questo metodo di accesso al db nelle web application!
    //WARNING: DO NOT use this db access method in web applications!
    private void action_query_JDBC_SQL_Precompiled(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException, ClassNotFoundException {
        String name = request.getParameter("n");

        HTMLResult result = new HTMLResult(getServletContext());
        result.setTitle("Search results (DIRECT CONNECTION, PRECOMPILED SQL)");

        //caricamento dinamico della classe driver. 
        //usiamo dei parametri del contesto per configurarlo (vedi web.xml)            
        //dynamic loading of the driver class
        //here we get the connection string, driver name, username and password 
        //from the context initialization parameters (see web.xml)
        Class.forName(getServletContext().getInitParameter("data.jdbc.driver"));

        //usiamo il try-with-resources per ottimizzare il processo di chiusura delle risorse
        try (
                //connessione al database locale
                //database connection
                Connection connection = DriverManager.getConnection(getServletContext().getInitParameter("data.jdbc.connectionstring"), getServletContext().getInitParameter("data.jdbc.username"), getServletContext().getInitParameter("data.jdbc.password"));
                //compiliamo la query parametrica
                //compile the parametric query
                PreparedStatement ps = connection.prepareStatement(SQL_SELECT_AUTHOR)) {

            //imposiamo il parametro al suo valore effettivo
            //set the parameter to its actual value
            ps.setString(1, "%" + name + "%");
            //eseguiamo la query (SQL diretto)
            //query execution (direct SQL)
            try (ResultSet rs = ps.executeQuery()) {
                print_author_results(name, rs, result);
                result.activate(request, response);
            }
        }
    }

    /*
     * ATTENZIONE: il codice di questo metodo dipende dalla corretta definizione delle
     * risorse presente nei file context.xml (Resource) e web.xml (resource-ref)
     * 
     * WARNING: this method needs the definition of an external data source to work correctly.
     * See the Resource element in context.xml and the resource-ref element in web.xml
     * 
     */
    private void action_query_Pool(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException, NamingException {

        String name = request.getParameter("n");

        HTMLResult result = new HTMLResult(getServletContext());
        result.setTitle("Search results (CONNECTION POOL)");

        ///preleviamo un riferimento al naming context
        //get a reference to the naming context
        InitialContext ctx = new InitialContext();
        //e da questo otteniamo un riferimento alla DataSource
        //che gestisce il pool di connessioni. 
        //usiamo un parametro del contesto per ottenere il nome della sorgente dati (vedi web.xml)

        //and from the context we get a reference to the DataSource
        //that manages the connection pool
        //we also use a context parameter to obtain the data source name (see web.xml)
        DataSource ds = (DataSource) ctx.lookup(getServletContext().getInitParameter("data.source"));
        //usiamo il try-with-resources per ottimizzare il processo di chiusura delle risorse
        try (
                //connessione al database locale
                //database connection
                Connection connection = ds.getConnection();
                //POSSIAMO USARE SIA L'SQL PRECOMPILATO CHE QUELLO DIRETTO
                //WE CAN USE BOTH PRECOMPILED OR DIRECT SQL HERE
                //compiliamo la query parametrica
                //compile the parametric query
                PreparedStatement ps = connection.prepareStatement(SQL_SELECT_AUTHOR)) {

            //imposiamo il parametro al suo valore effettivo
            //set the parameter to its actual value
            ps.setString(1, "%" + name + "%");
            //eseguiamo la query (SQL diretto)
            //query execution (direct SQL)
            try (ResultSet rs = ps.executeQuery()) {
                print_author_results(name, rs, result);
                result.activate(request, response);
            }
        }

    }


    /*
     * ATTENZIONE: il codice di questo metodo dipende dalla corretta definizione delle
     * risorse presente nei file context.xml (Resource) e web.xml (resource-ref)
     * 
     * WARNING: this method needs the definition of an external data source to work correctly.
     * See the Resource element in context.xml and the resource-ref element in web.xml
     * 
     */
    private void action_query_Pool_Injection(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        String name = request.getParameter("n");

        HTMLResult result = new HTMLResult(getServletContext());
        result.setTitle("Search results (INJECTED DATASOURCE)");

        //usiamo il try-with-resources per ottimizzare il processo di chiusura delle risorse
        try (
                //connessione al database locale
                //database connection
                Connection connection = dsg.getConnection();
                //POSSIAMO USARE SIA L'SQL PRECOMPILATO CHE QUELLO DIRETTO
                //WE CAN USE BOTH PRECOMPILED OR DIRECT SQL HERE
                //compiliamo la query parametrica
                //compile the parametric query
                PreparedStatement ps = connection.prepareStatement(SQL_SELECT_AUTHOR)) {

            //imposiamo il parametro al suo valore effettivo
            //set the parameter to its actual value
            ps.setString(1, "%" + name + "%");
            //eseguiamo la query (SQL diretto)
            //query execution (direct SQL)
            try (ResultSet rs = ps.executeQuery()) {
                print_author_results(name, rs, result);
                result.activate(request, response);
            }
        }
    }

    private void action_default(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HTMLResult result = new HTMLResult(getServletContext());
        result.setTitle("Cercami!");
        result.appendToBody("<form method=\"get\" action=\"cercami\">");
        result.appendToBody("<p>Search for you name in the author database: ");
        result.appendToBody("<input type=\"text\" name=\"n\"/>");
        result.appendToBody("<input type=\"hidden\" name=\"action\" value=\"search\"/>");
        result.appendToBody("</p>");
        result.appendToBody("<p>Database mode: ");
        result.appendToBody("<input type=\"radio\" name=\"mode\" value=\"dsql\" checked=\"checked\"/> Direct Connection, Direct SQL");
        result.appendToBody("<input type=\"radio\" name=\"mode\" value=\"psql\"/> Direct Connection, Precompiled SQL");
        result.appendToBody("<input type=\"radio\" name=\"mode\" value=\"pool\"/> Connection Pool (DataSource)");
        result.appendToBody("<input type=\"radio\" name=\"mode\" value=\"gpool\"/> Injected DataSource (Connection Pool)");
        result.appendToBody("</p>");
        result.appendToBody("<p>");
        result.appendToBody("<input type=\"submit\" name=\"s\" value=\"Lookup\"/>");
        result.appendToBody("</p>");
        result.appendToBody("</form>");
        result.activate(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) {
        try {
            if (request.getParameter("action") != null) {
                if (request.getParameter("action").equals("search") && request.getParameter("n") != null && !request.getParameter("n").isBlank()) {
                    action_query(request, response);
                } else {
                    ServletHelpers.handleError("Invalid action requested", request, response, getServletContext());
                }
            } else {
                action_default(request, response);
            }
        } catch (Exception ex) {
            ServletHelpers.handleError(ex, request, response, getServletContext());
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);

    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);

    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "An useful servlet";

    }// </editor-fold>
}
