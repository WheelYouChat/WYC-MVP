package com.wyc.sms.sender.telesign;

import com.telesign.MessagingClient;
import com.telesign.RestClient;

public class SendMessage {

    public static void main(String[] args) {

        String customerId = "718017CD-005E-4FD2-AD96-D6DE12B19E20";
        String apiKey = "vWiFfSSTCzyYWrTpPaa1YeCdO1K6ZF+xpzNgXaUWQBNFbDVdHdYvQ9Lj1hkHRc5L+7SYreNMTsmwu5FhG8UguA==";
        String phoneNumber = "+79112127484";
        String message = "Hello From LihaChat6";
        String messageType = "ARN";

        try {
            MessagingClient messagingClient = new MessagingClient(customerId, apiKey);
            RestClient.TelesignResponse telesignResponse = messagingClient.message(phoneNumber, message, messageType, null);
            System.out.println("Your reference id is: "+telesignResponse.json.get("reference_id"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
