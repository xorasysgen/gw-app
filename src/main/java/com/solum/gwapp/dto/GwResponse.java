package com.solum.gwapp.dto;

import lombok.Data;

@Data
public class GwResponse {
   private boolean result;
   private int resultCode;
   private String errorMessage;
}
