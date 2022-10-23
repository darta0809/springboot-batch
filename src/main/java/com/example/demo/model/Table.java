package com.example.demo.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class Table implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String name;
  private List<TableColumn> column;
}
