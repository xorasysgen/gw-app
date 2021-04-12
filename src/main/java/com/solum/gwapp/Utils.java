package com.solum.gwapp;

import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;

@Slf4j
public class Utils {
   public static  String getMD5Hash(String data) {
      String result = null;
      try {
         MessageDigest digest = MessageDigest.getInstance("MD5");
         byte[] hash = digest.digest(data.getBytes("UTF-8"));
         return DatatypeConverter.printHexBinary(hash);
      }catch(Exception ex) {
         log.error("MD5 failed : {}",ex.getMessage());
      return null;
      }
   }
}

