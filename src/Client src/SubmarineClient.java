import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

class SubmarineClient extends JFrame {
    static int inPort = 9997;
    static String address = "192.168.0.41";
    static public PrintWriter out;
    static public BufferedReader in;
    static String userName = "";
    static int num_mine = 10;
    static int width = 10;
    static int playerTurn = 0;
    static int minesFound = 0;
    static public JButton[] buttons;
    static public JLabel statusLabel;

    JFrame frame;
    public Container cont;
    private JPanel p1;
    private Socket socket;
    private boolean isMyTurn = true;

  
    
    Image image = new ImageIcon("./images/mine.png").getImage();
    Image scaledImage = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
    
    private ImageIcon mineIcon = new ImageIcon(scaledImage);
    
    public SubmarineClient(String userName) {
        SubmarineClient.userName = userName;

        frame = new JFrame();
        frame.setSize(700, 500);
        frame.setLocation(150, 150);
        frame.setTitle("밴드맨들의 두근두근 지뢰찾기 게임    " + userName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        cont = frame.getContentPane();
        cont.setLayout(new BorderLayout());
        cont.setBackground(new Color(0xADD8E6));  // 배경색 설정

        p1 = new JPanel(new GridLayout(width, width));
        buttons = new JButton[width * width];

        for (int i = 0; i < width * width; i++) {
            buttons[i] = new JButton();
            int finalI = i;
            buttons[i].addActionListener(e -> {
                if (isMyTurn) {
                    int x = finalI / width;
                    int y = finalI % width;
                    System.out.println("버튼이 클릭됨!: (" + x + ", " + y + ")");
                    guess(x, y, finalI);
                    
                } else {
                    System.out.println("너의 차례가 아님!");
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, "너의 차례가 아님!", "주의", JOptionPane.WARNING_MESSAGE);
                    });
                }
            });
            p1.add(buttons[i]);
        }

        statusLabel = new JLabel("지뢰를 찾아라");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        cont.add(statusLabel, BorderLayout.NORTH);
        cont.add(p1, BorderLayout.CENTER);

        frame.setVisible(true);

        startClient();
    }

    private void startClient() {
        new Thread(() -> {
            try {
                socket = new Socket(address, inPort);
                SubmarineClient.out = new PrintWriter(socket.getOutputStream(), true);
                SubmarineClient.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                System.out.println("서버에 연결 중");
                out.println(userName); // 클라이언트 이름 전송
                String msg = in.readLine(); // 서버로부터 초기 메시지 대기
                System.out.println(msg);
                msg = in.readLine(); // 시작 메시지 대기
                System.out.println(msg);

                while (true) {
                    msg = in.readLine();
                    if (msg == null) {
                        continue;
                    }
                    if (msg.equalsIgnoreCase("ok")) {
                        isMyTurn = false;
                        updateStatusLabel("상대방 차례를 기다리는 중...");
                    } else if (msg.startsWith("mine")) {
                        String[] arr = msg.split(",");
                        int x = Integer.parseInt(arr[1]);
                        SwingUtilities.invokeLater(() -> {
                        	buttons[x].setBackground(Color.RED);
                            buttons[x].setIcon(mineIcon); // 지뢰 이미지 설정
                            buttons[x].setDisabledIcon(mineIcon); // 비활성화된 상태에서도 동일한 아이콘 설정
                            buttons[x].setEnabled(false);
                            buttons[x].putClientProperty("fixed", true); // 색깔 고정 속성 추가
                            minesFound++;
                            updateGameStatus();
                        });
                        isMyTurn = false;
                    } else if (msg.startsWith("null")) {
                        String[] arr = msg.split(",");
                        int x = Integer.parseInt(arr[1]);
                        SwingUtilities.invokeLater(() -> {
                            if (!Boolean.TRUE.equals(buttons[x].getClientProperty("fixed"))) {
                                buttons[x].setBackground(Color.GRAY);
                                buttons[x].setEnabled(false);
                                updateGameStatus();
                            }
                        });
                        isMyTurn = false;
                    } else if (msg.startsWith("Location")) {
                        String[] arr = msg.split(",");
                        int x = Integer.parseInt(arr[1]);
                        SwingUtilities.invokeLater(() -> {
                            if (!Boolean.TRUE.equals(buttons[x].getClientProperty("fixed"))) {
                                //buttons[x].setText("X");
                                buttons[x].setBackground(Color.GRAY);
                                buttons[x].setEnabled(false);
                            }
                        });
                    } else if (msg.startsWith("Game Over")) {
                        String endmsg = in.readLine();
                        System.out.println(endmsg);
                        SwingUtilities.invokeLater(() -> {
                            for (JButton button : buttons) {
                                button.setEnabled(false);
                            }
                            JOptionPane.showMessageDialog(frame, endmsg, "게임 종료", JOptionPane.INFORMATION_MESSAGE);
                        });

                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    } else if (msg.equalsIgnoreCase("finish")) {
                        isMyTurn = true;
                    }
                }
            } catch (Exception e) {
                if (!socket.isClosed()) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void guess(int x, int y, int buttonIndex) {
        SwingUtilities.invokeLater(() -> {
            //buttons[buttonIndex].setText("X");
            buttons[buttonIndex].setEnabled(false); // 클릭 후 버튼 비활성화
        });

        new Thread(() -> {
            try {
                out.println(x + "," + y); // 좌표 전송
                String msg = in.readLine(); // 서버로부터 응답 대기

                if (msg.startsWith("mine")) {
                    SwingUtilities.invokeLater(() -> {
                    	buttons[buttonIndex].setBackground(Color.RED);
                        buttons[buttonIndex].setIcon(mineIcon); // 지뢰 이미지 설정
                        buttons[buttonIndex].setDisabledIcon(mineIcon); // 비활성화된 상태에서도 동일한 아이콘 설정
                        buttons[buttonIndex].setEnabled(false); // 버튼 비활성화
                        buttons[buttonIndex].putClientProperty("fixed", true); // 색깔 고정 속성 추가
                        minesFound++; // 발견한 지뢰 수 증가
                        updateGameStatus(); // 게임 상태 업데이트
                    });
                    isMyTurn = false;
                } else if (msg.startsWith("null")) {
                    SwingUtilities.invokeLater(() -> {
                        if (!Boolean.TRUE.equals(buttons[buttonIndex].getClientProperty("fixed"))) {
                            buttons[buttonIndex].setBackground(Color.GRAY);
                            buttons[buttonIndex].setEnabled(false);
                            updateGameStatus();
                        }
                    });
                    isMyTurn = false;
                } else if (msg.startsWith("Location")) {
                    String[] arr = msg.split(",");
                    int a = Integer.parseInt(arr[1]);
                    SwingUtilities.invokeLater(() -> {
                        if (!Boolean.TRUE.equals(buttons[a].getClientProperty("fixed"))) {
                           // buttons[a].setText("X");
                            buttons[a].setBackground(Color.GRAY);
                            buttons[a].setEnabled(false);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void updateStatusLabel(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    public static void updateGameStatus() {
        playerTurn++;
        SwingUtilities.invokeLater(() -> statusLabel.setText("차례: " + playerTurn + ", 찾은 지뢰 갯수: " + minesFound));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String userName = JOptionPane.showInputDialog(null, "사용자 이름을 입력하세요:", "로그인", JOptionPane.PLAIN_MESSAGE);
            if (userName != null && !userName.trim().isEmpty()) {
                new SubmarineClient(userName);
            } else {
                JOptionPane.showMessageDialog(null, "유효한 사용자 이름을 입력하세요.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
