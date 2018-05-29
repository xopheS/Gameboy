package ch.epfl.gameboj.mvc;

import static ch.epfl.gameboj.GameBoy.CYCLES_PER_NANOSECOND;
import static ch.epfl.gameboj.component.lcd.LcdController.IMAGE_CYCLE_DURATION;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.serial.SerialProtocol;
import ch.epfl.gameboj.gui.ImageConverter;
import ch.epfl.gameboj.mvc.controller.Controller;
import javafx.animation.AnimationTimer;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.scene.image.Image;

public final class Model {
	private static final Model model = new Model();
	private View view;
	private Controller controller = Controller.getController();
	
	private GameBoy gameboj;
	
    private LongProperty start = new SimpleLongProperty();
    
    private AnimationTimer animationTimer;
    
    long pauseTime;
    
    IntegerProperty cycleSpeed = new SimpleIntegerProperty(1);
    
    private static ServerSocket serverSocket;
    private static Socket clientSocket;
    
    private ObjectProperty<Image> screenImage;
	
	private Model() {
        start.set(System.nanoTime());
		
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsed = now - start.get() - pauseTime;
                long elapsedCycles = (long) (elapsed * CYCLES_PER_NANOSECOND);

                model.getGameboj().runUntil(cycleSpeed.get() * IMAGE_CYCLE_DURATION + model.getGameboj().getCycles());

                screenImage.set(ImageConverter.convert(gameboj.getLcdController().currentImage()));
            }
        };
        
        if (Controller.mustStart) {
        	animationTimer.start();
        }
	}
	
    public void initiateClient(InetAddress locIP) {
    	try {
    		clientSocket = new Socket(locIP, 4444);
    		
    		SerialProtocol.setDataInput(new DataInputStream(clientSocket.getInputStream()));
    		SerialProtocol.setDataOutput(new DataOutputStream(clientSocket.getOutputStream()));
    		
        	SerialProtocol.startProtocol(false);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }
    
    public void initiateServer(InetAddress locIP) {
    	Runnable r = new Runnable() {
        	@Override
			public void run() {
        		try {	
            		serverSocket = new ServerSocket(4444, 0, locIP);
        			Socket clientSocket = serverSocket.accept();
        			
        			SerialProtocol.setDataInput(new DataInputStream(clientSocket.getInputStream()));
        			SerialProtocol.setDataOutput(new DataOutputStream(clientSocket.getOutputStream()));
        			
                	SerialProtocol.startProtocol(true);
        		} catch (UnknownHostException e1) {
        			e1.printStackTrace();
        		} catch (IOException e1) {
        			e1.printStackTrace();
        		}
        	}
        };
        
        new Thread(r).start();
    }
	
	public static Model getModel() {
		return model;
	}
	
	public void setView(View view) {
		this.view = view;
	}
	
	public GameBoy getGameboj() {
		return gameboj;
	}
	
	public void setGameboj(GameBoy gameboj) {
		this.gameboj = gameboj;
	}
	
	public ObjectProperty<Image> getScreenImage() {
		return screenImage;
	}
	
	public Socket getClientSocket() {
		return clientSocket;
	}
	
	public ServerSocket getServerSocket() {
		return serverSocket;
	}
	
	public AnimationTimer getAnimationTimer() {
		return animationTimer;
	}
	
	public void setAnimationTimer(AnimationTimer animationTimer) {
		this.animationTimer = animationTimer;
	}
	
	public LongProperty getStart() {
		return start;
	}
	
	public IntegerProperty getCycleSpeed() {
		return cycleSpeed;
	}
}
