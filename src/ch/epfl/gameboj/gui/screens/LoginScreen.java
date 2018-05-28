package ch.epfl.gameboj.gui.screens;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public final class LoginScreen {
    private static ImageView shearedGameboyView = new ImageView();
	
	public static Scene getLoginScreen(Stage owner, Scene nextScreen, ResourceBundle guiBundle) {
		GridPane loginPane = new GridPane();
        loginPane.setAlignment(Pos.CENTER);
        loginPane.setHgap(10);
        loginPane.setVgap(10);
        loginPane.setPadding(new Insets(25, 25, 25, 25));
        
        Text loginText = new Text("Please log in");
        loginPane.add(loginText, 0, 0, 2, 1);
        
        Label usernameLabel = new Label(guiBundle.getString("username"));
        loginPane.add(usernameLabel, 0, 1);
        TextField usernameField = new TextField();
        loginPane.add(usernameField, 1, 1);
        
        Label passwordLabel = new Label(guiBundle.getString("password"));
        loginPane.add(passwordLabel, 0, 2);
        PasswordField passwordField = new PasswordField();
        loginPane.add(passwordField, 1, 2);
        
        Button createAccountButton = new Button("Create a new account");
        loginPane.add(createAccountButton, 1, 3);
        
        Button login = new Button("Log in");
        Image icon = new Image("File:cartoon-gameboy-gameboy-sbstn723-on-deviantart.png", 150, 150, true, true, true);
        
        Button noAccountButton = new Button("Continue without login");
        noAccountButton.setOnAction(e -> {
        	owner.setScene(nextScreen);
        });
        
        loginPane.add(noAccountButton, 2, 5);
        loginPane.add(login, 1, 5);
        PerspectiveTransform shear = new PerspectiveTransform();
        shear.setUlx(13);
        shear.setUly(-2);
        shear.setUrx(50);
        shear.setUry(0);
        shear.setLrx(37);
        shear.setLry(28);
        shear.setLlx(0);
        shear.setLly(25);
        shearedGameboyView.setEffect(shear);
        shearedGameboyView.setRotate(21);
        shearedGameboyView.setTranslateX(47);
        shearedGameboyView.setTranslateY(46);
        loginPane.add(new ImageView(icon), 2, 1, 1, 2);
        loginPane.add(shearedGameboyView, 2, 1, 1, 2); 
        
        Scene loginScreen = new Scene(loginPane);
        loginScreen.setOnKeyPressed(e -> {
        	if (e.getCode() == KeyCode.ENTER) {
        		try {
        			Connection connection = DriverManager.getConnection("jdbc:mysql://sql7.freemysqlhosting.net:3306/sql7239737", "sql7239737", "QTbGGaykPd");
        			PreparedStatement ps = 
        					connection.prepareStatement("SELECT `Username`, `Password` FROM `Gameboj Users` WHERE `Username` = ? AND `Password` = ?");
        			ps.setString(1, usernameField.getText());
        			ps.setString(2, passwordField.getText());
        			ResultSet result = ps.executeQuery();
        			if (result.next()) {
        				owner.setScene(nextScreen);
        			} else {
        				Text loginResultText = new Text("Username/password wrong");
        				loginPane.add(loginResultText, 0, 7);
        			}
        		} catch (SQLException ex) {
        			ex.printStackTrace();
        		}
        	}
        });
        
        Scene createAccountScreen = AccountCreationScreen.getAccountCreationScreen(owner, loginScreen);
        
        createAccountButton.setOnAction(e -> {
        	owner.setScene(createAccountScreen);
        });
        
        return loginScreen;
	}
	
	public static void setShearedGameboyView(Image source) {
		shearedGameboyView.setImage(source);
	}
}
