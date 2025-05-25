// enjoy submarine detection game

import java.util.Vector;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;


public class SubmarineServer {
   public static int inPort = 9997;
   public static Vector<Client> clients = new Vector<Client>();
   public static int maxPlayer=2;
   public static int numPlayer=0; 
   public static int width=10;
   public static int num_mine=10;
   public static Map map;
   int finish = 0;
   JFrame frame;
    public Container cont;
    public JPanel p0, p1;
    public JTextField t0,t1;
    public JButton[] buttons;

   
   public static void main(String[] args) throws Exception {
      new SubmarineServer().createServer();      
   }

   public void createServer() throws Exception {      
      System.out.println("서버 가동중..");
       ServerSocket server = new ServerSocket(inPort); 
       
       numPlayer=0;
        while (numPlayer<maxPlayer) {
           Socket socket = server.accept(); 
            Client c = new Client(socket);
            clients.add(c);
            numPlayer++;
        }
        System.out.println("\n"+numPlayer+" 명의 사용자가 왔습니다.");
        for(Client c:clients) {
           c.turn = true;
           System.out.println("  - "+c.userName);
        }
                
        map = new Map(width, num_mine);
        sendtoall("게임 시작~!");
        
        frame = new JFrame();
        frame.setSize(800, 600);
        frame.setLocation(150, 150);
        frame.setTitle("두근두근 지뢰찾기(서버)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   
        cont = frame.getContentPane();               
        cont.setLayout(new BorderLayout());
        
        
        JPanel p0 = new JPanel(new GridLayout(1,3));
        JLabel[] successLabels = new JLabel[maxPlayer];
        JLabel mineLabel = new JLabel("지뢰 갯수" + num_mine);
        for (int i = 0; i < maxPlayer; i++) {
            //successLabels[i] = new JLabel("Player " + (i+1) + " 성공: 0"); // 초기화된 성공 횟수 레이블 생성
           successLabels[i] = new JLabel("Player " + (i+1) + " 지뢰 찾은 횟수: 0");
            p0.add(successLabels[i]); // 패널에 추가
        }
        JPanel p3 = new JPanel(new GridLayout(1, maxPlayer));
        JLabel[] attemptLabels = new JLabel[maxPlayer];

        for (int i = 0; i < maxPlayer; i++) {
            attemptLabels[i] = new JLabel("Player " + (i+1) + " 시도횟수: 0");
            p3.add(attemptLabels[i]);
        }
        p0.add(mineLabel); 
        updateGUI(successLabels, attemptLabels, mineLabel);
        
        cont.add(p0, BorderLayout.NORTH);
        cont.add(p3, BorderLayout.SOUTH);
        
        JPanel p1 = new JPanel(new GridLayout(width, width));
        
              
                
        buttons = new JButton[width*width];
        for (int i=0; i<width*width; i++){
           
            int x = i/width;
            int y = i%width;
            if (map.checkMine(x, y)>=0)
                buttons[i] = new JButton("M");
            else
                buttons[i] = new JButton(" ");
            p1.add(buttons[i]);
        }
        //cont.validate();

        cont.add(p1,BorderLayout.CENTER);
        frame.setVisible(true);
                
        while(true) {
           if (allTurn()) {
              System.out.println();

              for(Client c : clients) {
                  int check=map.checkMine(c.x, c.y);
                 if (check>=0) {
                    System.out.println(c.userName + "(" + c.x+" , "+c.y+") 에서 지뢰를 찾음");
                    map.updateMap(c.x, c.y);
                    int location = (c.x*width) + c.y;
                    c.send("mine," + location);
                       sendtoall("Location,"+ location);
                    c.successCount++;
                    c.try_num++;
                    num_mine--;
                    if(c.successCount >= 3) {
                       sendtoall("Game Over");
                       String msg = "게임이 끝났습니다. " + c.userName + " 승리!";
                       System.out.println(msg);
                       sendtoall(msg);
                       SwingUtilities.invokeLater(() -> {
                           JOptionPane.showMessageDialog(frame, msg, "게임 종료", JOptionPane.INFORMATION_MESSAGE);
                       });
                      
                    }
                    
                    SwingUtilities.invokeLater(() -> {
                            buttons[location].setBackground(Color.RED);
                            buttons[location].setText(c.userName);
                            buttons[location].setForeground(Color.CYAN);
                            buttons[location].setEnabled(false); // 버튼 비활성화
                    });
           //
                    updateGUI(successLabels, attemptLabels, mineLabel);
                    
                 }
                 else {
                    System.out.println(c.userName + "(" + c.x+" , "+c.y+") 에 지뢰 없음");
                    c.try_num++;
                    int location = (c.x*width) + c.y;
                    c.send("null,"+ location);
                      sendtoall("Location," + location);
                      SwingUtilities.invokeLater(() -> {
                            buttons[location].setBackground(Color.GRAY);
                            buttons[location].setText(c.userName);
                            buttons[location].setForeground(Color.orange);
                            buttons[location].setEnabled(false); // 버튼 비활성화
                    });
                      updateGUI(successLabels, attemptLabels, mineLabel);
                    
                 }
                                      
                    c.turn=true;
                 
                 
              }sendtoall("finish");

           }
        } 
        
                
   }
   private void updateGUI(JLabel[] successLabels, JLabel[] attemptLabels, JLabel mineLabel) {
       for (int i = 0; i < clients.size(); i++) {
           successLabels[i].setText(clients.get(i).userName + " 지뢰 찾은 횟수: " + clients.get(i).successCount);
       }
       for(int i = 0; i< clients.size(); i++) {
          attemptLabels[i].setText(clients.get(i).userName + "시도 횟수: " + clients.get(i).try_num);
          
       }
       mineLabel.setText("지뢰 갯수: " + num_mine);
   }
   
   
   public void sendtoall(String msg) {
      for(Client c : clients) 
         c.send(msg);
   }
   
   
   public boolean allTurn() {
      int i=0;
      for(Client c:clients)
         if (c.turn == false)
            i++;
      
      if (i==clients.size()) return true;
      else return false;      
   }
   
   
   class Client extends Thread {
      Socket socket; 
      PrintWriter out = null; 
      BufferedReader in = null; 
      Map map;
      String userName = null;
      int x, y;
      public boolean turn=false;
      int successCount = 0;
      int try_num=0;
      
  
      
      public Client(Socket socket) throws Exception {
            initial(socket);               
            start();                                  
      }
      
      
      public void initial(Socket socket) throws IOException {
         this.socket = socket;
            out = new PrintWriter(socket.getOutputStream(), true); 
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     
            userName = in.readLine();
            System.out.println(userName+" 오셨습니다 "+socket.getInetAddress());
            send("플레이어 기다리는 중..");
          
      }
               
  
        @Override
        public void run() {
           String msg;

           try {
               while(true) {
                  msg = in.readLine();
                  if(msg == null) {
                     break;
                  }
                  if (turn) {  
                     String[] arr = msg.split(",");                  
                     x = Integer.parseInt(arr[0]);
                     y = Integer.parseInt(arr[1]);    
                     send("ok");
                     
                     turn=false;
                  }
                    
               }
            }
           catch (IOException e) { 
               //clients.remove(this);
           } 
        }
        
        
        public void send(String msg) {
         out.println(msg);
      }
              
   }
        
   
}
