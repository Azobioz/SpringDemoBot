package com.telegramtest.SpringDemoBot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.ToString;

import java.sql.Timestamp;

@Entity(name = "usersDataTable")
@Data
@ToString
public class User {

    @Id
    private long chatId;

    private String firstName;

    private String lastName;

    private String userName;

    private Timestamp registeredAt;


}
