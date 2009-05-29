signature A
= sig
  type T
  val mkA : T
  val destA : T -> T list
end;

signature B
= sig
  type T
  val mkB : T
  val destB : T -> T list
end;


functor AB(structure A : A and B : B) 
= struct 
  
  val mk1 = A.mkA;
    
  structure A = B
  
  val L = B.destB mk1;
  
end;


structure Bs : B where type T = string
= struct 
  open As;
  fun bfoo s = s ^ "b";
end;

structure SafeBs :> B = Bs;
