package echoserver;

import java.io.BufferedReader;
import java.net.*;
import java.io.*;
import java.sql.*;

public class EchoServerThread implements Runnable {
    protected Socket socket;

    public EchoServerThread(Socket clientSocket) {
        this.socket = clientSocket;
    }

    public void run() {
        //Deklaracje zmiennych
        BufferedReader brinp = null;
        DataOutputStream out = null;
        String threadName = Thread.currentThread().getName();

        //inicjalizacja strumieni
        try {
            brinp = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()
                    )
            );

            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println(threadName + "| Błąd przy tworzeniu strumieni " + e);
            return;
        }


        String currentbalance, currentbalance2, query, query1, query2, name, lastname, username, password, verify;
        float balance1 = 0, balance2 = 0, balance3 = 0;


//    pętla główna

        try {
            //laczenie z baza danych sql
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection myConn = DriverManager.getConnection("jdbc:mysql://localhost:3306/users", "root", "datadata1");
            Statement myStmt = myConn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            myConn.setAutoCommit(true);

            //pobieranie loginu i hasla od uzytkownika
            username = brinp.readLine();
            System.out.println(threadName + "| Odczytano login: " + username);
            password = brinp.readLine();
            System.out.println(threadName + "| Odczytano haslo: " + password);

            //Sprawdzanie poprawnosci loginu i hasla
            String check = "SELECT * from users_data where username = \"" + username + "\"" + "and password = \"" + password + "\"";
            if ((myStmt.executeQuery(check)).absolute(1)) {
                verify = "1";
                out.writeBytes(verify + "\n");
                System.out.println(threadName + "| Zalogowano pomyślnie.");

            } else {
                verify = "0";
                out.writeBytes(verify + "\n");
                System.out.println(threadName + "| Błędny login lub hasło.");
                System.out.println(threadName + "| Zakończenie pracy z klientem: " + socket);
                socket.close();
                return;
            }

            while (true) {
                //wybieranie operacji przez uzytkownika
                int choice = Integer.parseInt(brinp.readLine());

                if (choice == 1) {  //sprawdzenie stanu konta
                    System.out.println(threadName + "| Wybrano (Stan Konta) ");
                    currentbalance = "SELECT balance FROM users_data where username = \"" + username + "\"";
                    ResultSet res1 = myStmt.executeQuery(currentbalance);
                    while (res1.next()) {
                        balance1 = res1.getFloat("balance");
                    }
                    res1.close();
                    out.writeBytes(balance1 + "\n");
                    System.out.println(threadName + "| Obecny stan konta: " + balance1);
                    System.out.println(threadName + "| Zakończenie pracy z klientem: " + socket);
                    socket.close();
                    break;
                }
                if(choice == 2) {
                    System.out.println(threadName + "| Wybrano (Przelew) ");
                    name = brinp.readLine(); //odczytanie imienia odbiorcy
                    lastname = brinp.readLine(); //odczytanie nazwiska odbiorcy
                    int accnum = Integer.parseInt(brinp.readLine()); //odczytanie numeru konta do przelewu
                    float transfer = Float.parseFloat(brinp.readLine()); //odczytanie kwoty przelewu

                    currentbalance = "SELECT balance FROM users_data where username = \"" + username + "\"";
                    ResultSet res2 = myStmt.executeQuery(currentbalance);
                    while (res2.next())
                    {
                        balance1 = res2.getFloat("balance");
                    }

                    currentbalance2 = "SELECT balance FROM users_data where Account_number = \"" +  accnum + "\"";
                    ResultSet res3 = myStmt.executeQuery(currentbalance2);
                    while (res3.next())
                    {
                        balance2 = res3.getFloat("balance");
                    }
                    res3.close();

                    if(transfer > balance1){
                        out.writeBytes("0" + "\n");
                        System.out.println(threadName + "| Brak wystarczających środków!");

                    } else {
                        out.writeBytes("1" + "\n");
                        query1 ="UPDATE users.users_data SET Balance = '" + (balance1 - transfer) + "' WHERE (Username = '" +  username + "')";
                        query2 ="UPDATE users.users_data SET Balance = '" + (balance2 + transfer) + "' WHERE (Account_number = '" +  accnum + "')";

                        myStmt.executeUpdate(query1);
                        myStmt.executeUpdate(query2);

                        currentbalance = "SELECT balance FROM users_data where username = \"" + username + "\"";
                        ResultSet res4 = myStmt.executeQuery(currentbalance);
                        while (res4.next()) {
                            balance2 = res4.getFloat("balance");
                        }res4.close();

                        currentbalance2 = "SELECT balance FROM users_data where Account_number = \"" + accnum + "\"";
                        ResultSet res5 = myStmt.executeQuery(currentbalance2);
                        while (res5.next()) {
                            balance3 = res5.getFloat("balance");
                        }
                        res5.close();

                        out.writeBytes(balance2 + "\n");
                        System.out.println(threadName + " |Przelew wykonany pomyślnie. ");
                        System.out.println(threadName + "| Stan konta zwiększył się o: " + transfer + "\nBieżący stan konta: "+ balance3);
                        System.out.println(threadName + "| Stan konta zmniejszył się o: " + transfer + "\nBieżący stan konta: " + balance2);
                        System.out.println(threadName + "| Zakończenie pracy z klientem: " + socket);
                        socket.close();
                        break;
                    }
                }

                if (choice == 3) { //wyplata srodkow z konta
                    System.out.println(threadName + "| Wybrano (Wypłata)");
                    float withdraw = Float.parseFloat(brinp.readLine());
                    currentbalance = "SELECT balance FROM users_data where username = \"" + username + "\"";
                    ResultSet res6 = myStmt.executeQuery(currentbalance);
                    while (res6.next()) {
                        balance1 = res6.getFloat("balance");
                    }
                    res6.close();

                    if (withdraw > balance1) {
                        out.writeBytes("0" + "\n");
                        System.out.println(threadName + "| Brak wystarczających środków!");
                        System.out.println(threadName + "| Zakończenie pracy z klientem: " + socket);
                        socket.close();
                        break;
                    } else {
                        out.writeBytes("1" + "\n");
                        query = "UPDATE `users`.`users_data` SET `Balance` = \'" + (balance1 - withdraw) + "\' WHERE (`Username` = \'" + username + "\')";
                        myStmt.executeUpdate(query);
                        currentbalance = "SELECT balance FROM users_data where username = \"" + username + "\"";
                        ResultSet res7 = myStmt.executeQuery(currentbalance);
                        while (res7.next())
                        {
                            balance2 = res7.getFloat("balance");
                        }
                        res7.close();
                        out.writeBytes(balance2 + "\n");

                        System.out.println(threadName + "| Wypłacono " + withdraw);
                        System.out.println(threadName + "| Obecny stan konta "+ balance2);
                        System.out.println(threadName + "| Zakończenie pracy z klientem: " + socket);
                        socket.close();
                        break;
                    }
                }

                if (choice == 4) {   //wplata srodkow na konto
                    System.out.println(threadName + "| Wybrano (Wpłata)");
                    float remittance = Float.parseFloat(brinp.readLine());
                    currentbalance = "SELECT balance FROM users_data where username = \"" + username + "\"";
                    ResultSet res8 = myStmt.executeQuery(currentbalance);
                    while (res8.next())
                    {
                        balance1 = res8.getFloat("balance");
                    }
                    res8.close();
                    query = "UPDATE `users`.`users_data` SET `Balance` = \'" + (balance1 + remittance) + "\' WHERE (`Username` = \'" + username + "\')";
                    myStmt.executeUpdate(query);
                    currentbalance = "SELECT balance FROM users_data where username = \"" + username + "\"";
                    ResultSet res9 = myStmt.executeQuery(currentbalance);
                    while (res9.next())
                    {
                        balance2 = res9.getFloat("balance");
                    }
                    res9.close();
                    out.writeBytes(balance2 + "\n");

                    System.out.println(threadName + "| Wpłacono " + remittance);
                    System.out.println(threadName + "| Obecny stan konta "+ balance2);
                    System.out.println(threadName + "| Zakończenie pracy z klientem: " + socket);
                    socket.close();
                    break;
                }

                if (choice == 5) {
                    System.out.println(threadName + "| Wybrano (Wyloguj)");
                    System.out.println(threadName + "| Zakończenie pracy z klientem: " + socket);
                    socket.close();
                    break;
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
}









