package com.example.demo.model;

import java.io.Serializable;
import lombok.Data;

@Data
public class Root implements Serializable {

  private Input input;
  private Output output;
  private Table table;
}
