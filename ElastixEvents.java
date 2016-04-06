package elastixlog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.event.HangupEvent;
import org.asteriskjava.manager.event.ManagerEvent;





public class ElastixEvents implements ManagerEventListener
{
    private ManagerConnection managerConnection;
    private String elastixServer = "localhost";
    public static String USER = "root";
    public static String PASS = "admin";
    
    public static String strConnection_mysql = "jdbc:mysql://mysql.firstcry.cc/asteriskcdrdb?zeroDateTimeBehavior=exception";

    public ElastixEvents()
    {        
        ManagerConnectionFactory factory = new ManagerConnectionFactory(elastixServer, 5038, "admin", "admin");
        managerConnection = factory.createManagerConnection();                
    }

    public void run() throws IOException, AuthenticationFailedException,
            TimeoutException, InterruptedException
    {
        // register for events
        managerConnection.addEventListener(this);
                
        // connect to Asterisk and log in
        managerConnection.login();
    }

    @Override
    public void onManagerEvent(ManagerEvent me) {
        //System.out.println(me.toString());  
        try
        {         
        switch(me.getClass().getName())
        {    
            case "org.asteriskjava.manager.event.HangupEvent":       
                 org.asteriskjava.manager.event.HangupEvent HangupEvnt = (HangupEvent)me;   
                 String dial_num="";
                 int dialed=0;
                 if(HangupEvnt.getCause()==28 && HangupEvnt.getChannel().toLowerCase().startsWith("dahdi/")){
                     dialed = MySQLcon(strConnection_mysql,"0"+HangupEvnt.getCallerIdNum());
                     /*if(HangupEvnt.getCallerIdNum().startsWith("0"))
                     { 
                         dial_num=HangupEvnt.getCallerIdNum();
                     }
                     else{
                         dial_num = "0"+HangupEvnt.getCallerIdNum().substring(HangupEvnt.getCallerIdNum().length()-10);
                     }*/
                         String strCall = "Action: Originate"
                                //+ "\r\nChannel: SIP/09021540686@tataOutSIP"
                                + "\r\nChannel: DAHDI/g0/0"+ HangupEvnt.getCallerIdNum()
                                + "\r\nContext: FailedCallCallback"
                                + "\r\nExten: 100\r\nPriority: 1"
                                + "\r\nCallerid: 0"+HangupEvnt.getCallerIdNum()//BillMobileNo
                                + "\r\nAsync: yes"
                                //+ "\r\nVariable: __SIPADDHEADER51=P-Preferred-Identity:<sip:66806900@10.0.90.2>"
                                + "\r\nVariable: OLDUID= " + HangupEvnt.getUniqueId()
                                + "\r\nActionID: 28\r\n\r\n";
                   if(Calendar.getInstance().getTime().getHours()>=21 || Calendar.getInstance().getTime().getHours()<9)
                        { 
                            Thread.sleep(1000*60);
                            System.out.println(HangupEvnt.getCallerIdNum().substring(HangupEvnt.getCallerIdNum().length()-10)+" Called between 9 am to 9 pm");
                            break;
                        }
                   if( HangupEvnt.getCallerIdNum().length() <= 11 ){
                       
                       //WriteToFIle("Dialed number :0"+ HangupEvnt.getCallerIdNum()+"\n");
                       WriteToFIle("Count :"+ dialed+"\n");
                       if(dialed<3){
                        DialNumber(strCall,"localhost");
                       }
                   }
                  System.out.println(strCall+"\n");
                 }
                 break;       
        }
        
        }
        catch(Exception ex)
        {
            System.out.println(me.toString());
            System.out.println(ex.getMessage());
        }
    
    
    
    }
    public static void DialNumber(String dt,String ip)
    {
        try
        {
            WriteToFIle(dt+"\n");
            java.util.Date curDate = new java.util.Date();
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
            String DateToStr = format.format(curDate);
            System.out.println("Calling at "+DateToStr+"\n");
            DateToStr = null;
            format = null;
            curDate = null;
            String a="http://"+ip+"/CODConfirmationDialer.php";
            URL url = new URL(a);
            URLConnection conn = url.openConnection();
            //url = null;
            String data = URLEncoder.encode("originateCmd", "UTF-8") + "=" + URLEncoder.encode(dt, "UTF-8");
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            wr.close();
            // open the stream and put it into BufferedReader
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                    //System.out.println(inputLine);
                    inputLine = inputLine + inputLine;                   
            }
            //WriteToFIle("\n"+inputLine);
            br.close();
            br = null;
           
        }catch(Exception ex)
        {
              java.util.Date curDate = new java.util.Date();
              SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
              String DateToStr = format.format(curDate);
              System.out.println("Exception in DialNumber : "  + ex.getMessage()+" at "+DateToStr);
              DateToStr = null;
              format = null;
              curDate = null;
            try {  
                Thread.sleep(1000*60);
            } catch (InterruptedException ex1) {}
        }
    }
    public static void WriteToFIle(String lines)
    {
      try{
                java.util.Date curDate = new java.util.Date();
                SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd kk:mm:ss");
                String DateToStr = format.format(curDate);
                
    		String data = " This content will append to the end of the file";
    		
    		File file =new File("FailedCallback.log");
    		
    		//if file doesnt exists, then create it
    		if(!file.exists()){
    			file.createNewFile();
    		}
    		
    		//true = append file
    		FileWriter fileWritter = new FileWriter(file.getName(),true);
    	        BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
    	        bufferWritter.write(lines+"\n"+DateToStr);
    	        bufferWritter.close();
    	    
	        System.out.println(data);
                DateToStr = null;
                format = null;
                curDate = null;       
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }
    public static int MySQLcon(String strConnection_mysql,String callerid )
    {
      Connection conn = null;
      Statement stmt = null;
      int cnt=0;
      try{
        Class.forName("com.mysql.jdbc.Driver");
        //WriteToFIle("Caller id in query : "+callerid);
        //System.out.println("Connecting to database...");
        conn = DriverManager.getConnection(strConnection_mysql,USER,PASS);
        //System.out.println("Creating statement...");
        stmt = conn.createStatement();
        String sql = "select count(1) as cnt from asteriskcdrdb.OriginateResponse o where o.CallerIdNum = \""+callerid+"\" and date(o.createdDateTime)=curdate() and o.Context='FailedCallCallBack';";
        //WriteToFIle(sql+"\n");
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()){                    
                cnt =  rs.getInt("cnt");              
        }
        rs.close();
        //WriteToFIle("Count is : "+cnt+"\n");
       }
      catch(Exception e){
            Date curDate = new Date();
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
            String DateToStr = format.format(curDate);
            System.out.println("Exception occur in ArrayList MySQL Dialer " + e.getMessage()+" at "+DateToStr);
            WriteToFIle("Exception occur in ArrayList MySQL Dialer!\n" + e.getMessage()+"\n");
                
            DateToStr = null;
            format = null;
            curDate = null;
            //sendsms(sendurl);
      }
      finally{
                try{
                   if(stmt!=null)
                      stmt.close();
                      }
                  catch(SQLException se2){
                  }
                try{
                   if(conn!=null)
                      conn.close();
                }catch(SQLException se){
                   System.out.println("Exception occur " + se.getMessage());
                   WriteToFIle("Exception occur!\n" + se.getMessage()+"\n");
                }//end finally tr
    }
       //WriteToFIle("Count is : "+cnt);
       return cnt;
  }

}