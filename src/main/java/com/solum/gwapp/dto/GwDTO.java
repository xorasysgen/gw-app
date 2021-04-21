package com.solum.gwapp.dto;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
public class GwDTO {

   @Id
   @GeneratedValue(strategy= GenerationType.AUTO)
   private Integer Id;
   private String gwIP;
   private String period;
   private String count;
}
