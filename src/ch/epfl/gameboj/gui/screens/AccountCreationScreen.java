package ch.epfl.gameboj.gui.screens;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public final class AccountCreationScreen {
	public static Scene getAccountCreationScreen(Stage owner, Scene nextScreen) {
		GridPane accountCreationPane = new GridPane();
        Scene createAccountScene = new Scene(accountCreationPane);
        Label nameLabel = new Label("What is your name?");
        accountCreationPane.add(nameLabel, 0, 0);
        TextField nameField = new TextField();
        accountCreationPane.add(nameField, 0, 1);
        Label createUsernameLabel = new Label("Pick a username:");
        accountCreationPane.add(createUsernameLabel, 0, 2);
        TextField usernameCreationField = new TextField();
        accountCreationPane.add(usernameCreationField, 0, 3);
        Label createPasswordLabel = new Label("Pick a password:");
        accountCreationPane.add(createPasswordLabel, 0, 4);
        PasswordField passwordCreationField = new PasswordField();
        accountCreationPane.add(passwordCreationField, 0, 5);
        Button accountDoneButton = new Button("Create your account");
        accountDoneButton.setOnAction(e -> {
        	try {
				Connection connection = DriverManager.getConnection("jdbc:mysql://sql7.freemysqlhosting.net:3306/sql7239737", "sql7239737", "QTbGGaykPd");
				PreparedStatement ps = connection.prepareStatement("INSERT INTO `Gameboj Users` VALUES (?, ?, ?)");
				ps.setString(1, nameField.getText());
				ps.setString(2, usernameCreationField.getText());
				ps.setString(3, passwordCreationField.getText());
				ps.execute();
				owner.setScene(nextScreen);
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        });
        accountCreationPane.add(accountDoneButton, 0, 6);
        
        return createAccountScene;
	}
}
