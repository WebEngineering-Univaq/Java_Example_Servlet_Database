#  Java_Example_Servlet_Database
> Basic  database access procedures in Java servlets
 
This example shows how to create a JDBC connection pool in Tomcat and use it in a web application through the DataSource objects.

## Usage

This is a *sample application* developed during the lectures of the  [**Web Engineering course**](https://webengineering-univaq.github.io). The code is organized to best match the lecture topics and examples. It is not intended for production use and is not optimized in any way. 

*This example code will be shown and described approximately during the 12th lecture of the course, so wait to download it, since it may get updated in the meanwhile.*

## Installation

This is a Maven-based project. Simply download the code and open it in any Maven-enabled IDE such as Netbeans or Eclipse. 

*Please do not download the code from the main branch, but from the branch corresponding to the platform used in the lectures:**
- the **JEE** branch contains the application version to be run on the **JavaEE 8** platform inside **Apache Tomcat version 9**. 
- the **JKEE** branch contains the application version to be run on the **JakartaEE 10** platform inside **Apache Tomcat version 10**. 

Note that you may need to *configure the deploy settings* based on the chosen platform/server: refer to your IDE help files to perform this step. For example, in Apache Netbeans, you must enter these settings in Project properties > Run.

Finally, this example uses a MySQL database. Therefore, you need a working instance of **MySQL version 8 or above**. Instructions on how to setup the sample database are embedded as comments in the class code.

 
---

![University of L'Aquila](https://www.disim.univaq.it/skins/aqua/img/logo2021-2.png)

 
