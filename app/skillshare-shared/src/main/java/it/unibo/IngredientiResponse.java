package it.unibo;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class IngredientiResponse implements Serializable{

    public List<String> ingredienti;
    public List<Integer> grammi;
    public Integer status;
    
    
}
