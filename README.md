Chisel template
=======================

### 1. Code folder tree

![sbt folder tree](https://astrohan.files.wordpress.com/2022/10/image.png)



### 2. Generate FIRRTL and Verilog code
There are two ways to run chisel
 - batch mode (run slowly)
    - ``sbt -v "runMain <PACKAGE_NAME>.<DESIGN_NAME>``
 - sbt shell (Recommand, more faster)
    - step1. enter **sbt shell** : ``sbt``
    - step2. run command : ``runMain <PACKAGE_NAME>.<DESIGN_NAME>``

```sh
# Ex) batch mode 
sbt -v "runMain exercise.RfFifo"

# Ex) sbt shell
sbt
sbt:chisel_template> runMain example.RfFifo
```

Once you've compiled, You can find the generated verilog in **./generated**


