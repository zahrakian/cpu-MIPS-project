package com.company;

        import java.io.File;
        import java.io.FileNotFoundException;
        import java.io.IOException;
        import java.io.RandomAccessFile;
        import java.util.Scanner;

public class Main {
    static String instructPath = "D:/darsi/java/Project_Memari/instructions2.txt";
    static String progressPath = "D:/darsi/java/Project_Memari/progress2.txt";
    static String regPath = "D:/darsi/java/Project_Memari/regs2.txt";
    static String memPath = "D:/darsi/java/Project_Memari/memory2.txt";
    static int PC = 0;
    static  int clk=0;


    static String ins;

    static PipelinedRegs if_id = new PipelinedRegs();
    static PipelinedRegs id_ex= new PipelinedRegs();
    static PipelinedRegs ex_mem= new PipelinedRegs();
    static PipelinedRegs mem_wb= new PipelinedRegs();

    static int ifNop = 1;
    static int idNop = 1;
    static int exNop = 1;
    static int memNop = 1;
    static int wbNop = 1;


    static int hazard = 0;

    static String pr[];

    static int numOfInst = 0;

    public static void instructionFetch(int instNum) throws IOException {
        if (ifNop == 0) {
            File r = new File(instructPath);
            Scanner sc = new Scanner(r);
            ins = sc.next();
            for (int i = 1; i <= PC; i++) {
                ins = sc.next();
            }
            PC++;


            numOfInst++;

            progress(numOfInst,clk,"IF");
            idNop = 0;
        } else {
            idNop = 1;
        }
    }

    public static void instructionDecode(int instNum) throws IOException {
        if (if_id.nop == 0) {
            String instruction = if_id.instruction;

            controlUnit(instruction);

            progress(if_id.numOfInst,clk,"ID");


            exNop = 0;
        } else {
            exNop = 1;
        }

    }


    static int rd_rt;

    static int rt;
    static int rd;
    public static void execute(int instNum) throws IOException {
        if (id_ex.nop == 0) {
            int aluSrc = id_ex.aluSrc;
            int aluOp = id_ex.ALUOp;
            int regDst = id_ex.regDst;
            int func = id_ex.func;
            int firstAlu = id_ex.rsValue;
            int secondAlu;

            //multiplexer ALUSrc
            if (aluSrc == 1) {
                secondAlu = id_ex.immidiate;
            } else {
                secondAlu = id_ex.rtValue;
            }


            //multiplexer RegDst
            if (regDst == 1) {
                rd_rt = id_ex.rd;
                rd= id_ex.rd;

            } else {
                rd_rt = id_ex.rt;
                rt = id_ex.rt;
            }

            int aluControlInput = aluControll(aluOp, func);
            alu(firstAlu, secondAlu, aluControlInput);



            progress(id_ex.numOfInst,clk,"EX");
            memNop = 0;
        } else {
            memNop = 1;
        }


    }

    static int readData;
//signextend function
    public static String extend(int val) {
        String binStr = Integer.toBinaryString(val);
        while (binStr.length() < 32) {
            binStr = "0" + binStr;
        }

        return binStr;
    }

    public static void memory(int instNum) throws IOException {
        if (ex_mem.nop == 0) {
            //calculating address of jump

            String address = extend(ex_mem.address);



            if (ex_mem.branch == 1 && ex_mem.zero == 1) {
                PC = ex_mem.PC;
            }
            if (ex_mem.jump == 1) {
                PC = Integer.parseInt(address, 2);
            }


            // for sw when memWrite is 1
            if (ex_mem.memWrite == 1) {
                putVal(memPath, ex_mem.aluResult, ex_mem.rtValue);
            }

            // for lw when memRead is 1
            else if (ex_mem.memRead == 1) {
                readData = getVal(memPath, ex_mem.aluResult);

            }

            progress(ex_mem.numOfInst,clk,"ME");

            wbNop = 0;

        } else {
            wbNop = 1;
        }

    }

    public static void writeBack(int instNum) throws IOException {
        if (mem_wb.nop == 0) {
            if (mem_wb.regWrite == 1) {
                if (mem_wb.memToReg == 1) {
                    putVal(regPath, mem_wb.rd_rt, mem_wb.readData);
                } else if (mem_wb.memToReg == 0) {
                    putVal(regPath, mem_wb.rd_rt, mem_wb.aluResult);
                }
            }
            progress(mem_wb.numOfInst,clk,"WB");



        }
    }




    public static void putVal(String path,int num, int val) throws IOException {
        String binStr = Integer.toBinaryString(val);
        while (binStr.length() < 32) {
            binStr = "0" + binStr;
        }


        RandomAccessFile raf = new RandomAccessFile(path, "rw");
        int i=0;
        int count=0;
        while (count < num) {
            i = i + 34;
            count++;
        }
        raf.seek(i);
        raf.writeBytes(binStr);
        raf.close();

    }

    public static int getVal(String path,int num) throws FileNotFoundException {
        File r = new File(path);
        Scanner sc = new Scanner(r);
        String reg = sc.next();
        for (int i=1;i<=num;i++){
            reg = sc.next();
        }
        int val = Integer.parseInt(reg,2);
        sc.close();
        return val;
    }
    static int aluOp;
    static int regDst;
    static int aluSrc;
    static int branch;
    public static int memRead;
    static int memWrite;
    static int memToReg;
    static int regWrite;
    static int jump;

    public static void controlUnit(String instruction) {
        if (Integer.parseInt(instruction.substring(0, 6), 2) == 0) {
            // based on opcode we figure out it's R-Type
            // then we can specify all
            aluOp = 2;

            regDst = 1;

            aluSrc = 0;

            branch = 0;

            memRead = 0;

            memWrite = 0;

            memToReg = 0;

            regWrite = 1;

            jump = 0;
        } else if (Integer.parseInt(instruction.substring(0, 6), 2) == 35) {
            //so it's lw

            aluOp = 0;

            regDst = 0;

            aluSrc = 1;

            branch = 0;

            memRead = 1;

            memWrite = 0;

            memToReg = 1;

            regWrite = 1;

            jump = 0;

        } else if (Integer.parseInt(instruction.substring(0, 6), 2) == 43) {
            // so it's sw
            aluOp = 0;


            aluSrc = 1;

            branch = 0;

            memRead = 0;

            memWrite = 1;


            regWrite = 0;

            jump = 0;

        } else if (Integer.parseInt(instruction.substring(0, 6), 2) == 2) {
            // it's jump ^^
            jump = 1;
        } else if (Integer.parseInt(instruction.substring(0, 6), 2) == 4) {
            // so it's beq :|

            aluOp = 1;


            aluSrc = 0;

            branch = 1;

            memRead = 0;

            memWrite = 0;


            regWrite = 0;

            jump = 0;


        }


    }


    // ALU
    static int aluResult;
    static int zero;
    public static void alu(int first, int second, int input) {
        if (input == 2) {
            // so it's add
            aluResult = first + second;
        } else if (input == 6) {
            // so it's sub
            aluResult = first - second;
            if (aluResult == 0) {
                zero = 1;
            } else {
                zero = 0;
            }
        } else if (input == 0) {
            // so it's and
            aluResult = first & second;

        } else if (input == 1) {
            // so it's or
            aluResult = first | second;
        } else if (input == 7) {
            // so it's slt
            if (first < second) {
                aluResult = 1;
            } else {
                aluResult = 0;
            }
        } else if (input == 12) {
            // so it's nor
            aluResult = ~(first | second);
        }
        else if (input == 13) {
            // so it's xor
            aluResult = ((~first)& second)|(first &(~second));
        }
    }

    // ALu Controll Unit
    public static int  aluControll(int op,int func) {
        int input;
        // by default we suppose op=0
        //so it's lw , sw
        // it just need add
        input = 2;

        if (op == 1) {
            // so it's beq
            // it's sub
            input = 6;
            return input;
        } else if (op == 2) {

            // so it's R-Type
            if (func == 32) {
                // so it's add
                input = 2;
                return input;

            } else if (func == 34) {
                //so it's sub
                input = 6;
                return input;
            } else if (func == 37) {
                //so it's or
                input = 1;
                return input;
            } else if (func == 39) {
                // so it's nor
                input = 12;
                return input;

            } else if (func == 36) {
                // so it's and
                input = 0;
                return input;
            } else if (func == 42) {
                //so it's slt
                input = 7;
                return input;
            }

        }
        return input;
    }

    public static void update() throws FileNotFoundException {


        //Mem update
        if (ex_mem.nop == 0) {
            mem_wb.regWrite = ex_mem.regWrite;
            mem_wb.memToReg = ex_mem.memToReg;
            mem_wb.aluResult = ex_mem.aluResult;
            mem_wb.rd_rt = ex_mem.rd_rt;

            if (id_ex.regDst == 1) {
                mem_wb.rd = ex_mem.rd;
            } else {
                mem_wb.rt = ex_mem.rd;
            }

            mem_wb.readData = readData;

            mem_wb.numOfInst = ex_mem.numOfInst;

        }


        mem_wb.nop = wbNop;

        // EX update


        if (id_ex.nop == 0) {


            ex_mem.rd_rt = rd_rt;

            if (id_ex.regDst == 1) {
                ex_mem.rd = rd;
            } else {
                ex_mem.rt = rt;
            }


            ex_mem.rtValue = id_ex.rtValue;
            ex_mem.branch = id_ex.branch;


            ex_mem.PC = id_ex.PC + id_ex.immidiate;
            ex_mem.memRead = id_ex.memRead;
            ex_mem.memWrite = id_ex.memWrite;
            ex_mem.memToReg = id_ex.memToReg;
            ex_mem.regWrite = id_ex.regWrite;

            ex_mem.jump = id_ex.jump;
            ex_mem.address = id_ex.address;

            // alu update
            ex_mem.aluResult = aluResult;
            ex_mem.zero = zero;

            ex_mem.numOfInst = id_ex.numOfInst;


        }


        ex_mem.nop = memNop;







        // ID update

        if (if_id.nop == 0) {
            // controll unit update

            id_ex.ALUOp = aluOp;

            id_ex.regDst = regDst;

            id_ex.aluSrc = aluSrc;

            id_ex.branch = branch;

            id_ex.memRead = memRead;

            id_ex.memWrite = memWrite;

            id_ex.memToReg = memToReg;

            id_ex.regWrite = regWrite;

            id_ex.jump = jump;


            id_ex.PC = if_id.PC;
            id_ex.func = Integer.parseInt(if_id.instruction.substring(26), 2);
            id_ex.immidiate = Integer.parseInt(if_id.instruction.substring(16), 2);

            id_ex.rs = Integer.parseInt(if_id.instruction.substring(6, 11), 2);
            id_ex.rsValue = getVal(regPath, id_ex.rs);

            id_ex.rt = Integer.parseInt(if_id.instruction.substring(11, 16), 2);
            id_ex.rtValue = getVal(regPath, id_ex.rt);

            id_ex.rd = Integer.parseInt(if_id.instruction.substring(16, 21), 2);

            id_ex.address = Integer.parseInt(if_id.instruction.substring(6), 2);

            id_ex.numOfInst = if_id.numOfInst;

        }

        hazardDetect();
        id_ex.nop = exNop;

        // if update
        if (ifNop == 0) {
            if_id.instruction = ins;
            if_id.PC = PC;
            if_id.numOfInst = numOfInst;



        }
        if_id.nop = idNop;
    }

    static String h;

    public static void hazardDetect() {

        if ((ex_mem.rd == id_ex.rs) && id_ex.rs!=0) {
            hazard = 1;
            h = "hazard clk" + (clk);
        }
        if ((ex_mem.rd == id_ex.rt) && id_ex.rt!=0) {
            hazard = 1;
            h = "hazard clk" + (clk);
        }
        if ((mem_wb.rd == id_ex.rs) && id_ex.rs!=0) {
            hazard = 1;
            h = "hazard clk" + (clk);
        }
        if ((mem_wb.rd == id_ex.rt) && id_ex.rt!=0) {
            hazard = 1;
            h = "hazard clk" + (clk);
        }


    }

    public static int getInstNum() throws FileNotFoundException {
        File r = new File(instructPath);
        Scanner sc = new Scanner(r);
        int i = 0;
        while (sc.hasNextLine()) {
            sc.nextLine();
            i++;
        }
        sc.close();
        return i;
    }

    public static void progress(int instNum,int clk,String stage) throws IOException {
        String s = stage + instNum + "-";
        pr[clk - 1] = pr[clk - 1] + s;
    }

    public static void printProgress(int size) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(progressPath,"rw");
        for (int i = 0; i < size; i++) {
            raf.writeBytes(pr[i]);
            raf.writeBytes("\r\n");

        }
    }


    public static void main(String[] args) throws IOException {
        // write your code here
        if_id.nop = 1;
        id_ex.nop = 1;
        ex_mem.nop = 1;
        mem_wb.nop = 1;



        int numberOfInst = getInstNum();

        int prSize = numberOfInst + 4;
        pr = new String[prSize];
        for (int j = 0; j < (prSize); j++) {
            pr[j] = "";
        }


        for (int i = 0; i < (numberOfInst + 4); i++) {
            hazard = 0;
            clk++;
            if (i < numberOfInst) {
                ifNop = 0;
            } else {
                ifNop = 1;
            }
            instructionFetch(i);
            instructionDecode(i);
            execute(i);
            memory(i);
            writeBack(i);
            update();
            System.out.print("Clk"+(i+1)+"==");

            System.out.println(pr[i]);

            if (hazard == 1) {
                System.out.println(h);
            }


        }
        printProgress(prSize);

    }
}
