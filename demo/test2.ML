structure foo = PolyML;

fun loopy n = 
    1 + loopy 
    (n + 1);
    
fun loopy2 n = 
    1 + loopy
    (n + 1);
    
    (* 
loopy 100; *)
