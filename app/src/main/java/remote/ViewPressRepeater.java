package remote;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import activity.MainActivity;

class ViewPressRepeater extends Thread {

    private final String command;

    public ViewPressRepeater(String command){
        this.command = command;
    }

    public void run(){
        do{
            sendControl(command);
            try{
                Thread.sleep(200);
            } catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        while (Remote.repeat);
    }

    private void sendControl(String command) {

        int portNum = 3995;
        String[] commandArray = {command, MainActivity.myPhone.getPhoneName()};

        try {
            Socket socket = new Socket(MainActivity.myPhone.getComputerIP(), portNum);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

            objectOutputStream.writeObject(commandArray);

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
