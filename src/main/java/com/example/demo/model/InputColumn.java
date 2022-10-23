package com.example.demo.model;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

@Data
public class InputColumn implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String name;
  private String type;
  private String length;
  private String quoted;
  private String nullable;
}
