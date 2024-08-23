import java.util.*;
import java.io.*;

class Tester
{
    public static void main(String[] args) throws IOException 
    {
        

        Disassembler d = new Disassembler(args[0]);

        
    }

}





//--------------------------DISSASSEMBLER-------------------






class Disassembler
{
    public byte[] binaryInput;
    HashMap<Integer, String> opHash = new HashMap<Integer, String>();
    ArrayList<String> assemblyOut = new ArrayList<String>();
    HashMap<Integer, String> labelHash = new HashMap<Integer, String>();


    public Disassembler(String fileName) throws IOException
    {

        Helper.fillOpcodeHash(opHash);
        binaryInput = Helper.readInputFromFilename(fileName);
        int[] binArr = Helper.byteArrayToInt(binaryInput);
        runner(binArr);

        for(int i = 0; i< assemblyOut.size(); i++)
        {
            if(labelHash.containsKey(i))
                System.out.println(labelHash.get(i)+":");
            System.out.println(assemblyOut.get(i));
        }
    }

 
    //---------------runner---------------


    public void runner(int[] binArr)
    {

        //run through line
        for(int i = 0; i < binArr.length/4; i++)
        {
            //get line
            int[] lineBytes = Helper.getFourByteLine(binArr, i*4);

            //PARSE opcode
            //CHECK against opHash
            String nameNType = parseInstruction(lineBytes);

            //use the type to choose the function processing
            String lastTwo = nameNType.substring(nameNType.length() - 2);
            String lastOne = nameNType.substring(nameNType.length() - 1);
            String cutoff = nameNType.substring(0, nameNType.length()-1);
    
    


            if(lastTwo.equals("cb"))
            {
                cutoff = cutoff.substring(0, cutoff.length()-1);
                assemblyOut.add(cbType(cutoff, lineBytes, i));
            }
            else if(lastOne.equals("r"))
            {
                assemblyOut.add(rType(cutoff, lineBytes));
            }
            else if(lastOne.equals("i"))
            {
                assemblyOut.add(iType(cutoff, lineBytes));
            }
            else if(lastOne.equals("b"))
            {
                assemblyOut.add(bType(cutoff, lineBytes, i));
            }
            else if(lastOne.equals("d"))
            {
                assemblyOut.add(dType(cutoff, lineBytes));
            }
        }
    }


    //------------instruction parsing-----------------


    public String parseInstruction(int[] line)
    {
        //try all levels of opcodes till match or no match
        int six = Helper.bitParser(line, 0, 6);
        int eight = Helper.bitParser(line, 0, 8);
        int ten = Helper.bitParser(line, 0, 10);
        int eleven = Helper.bitParser(line, 0, 11);

        String nameNType = null;

        //check if opHash map
        if(opHash.containsKey(six))
            nameNType = opHash.get(six);        
        else if(opHash.containsKey(eight))
            nameNType = opHash.get(eight);        
        else if(opHash.containsKey(ten))
            nameNType = opHash.get(ten);        
        else if(opHash.containsKey(eleven))
            nameNType = opHash.get(eleven);
        else
            nameNType = "NOMATCHr";
        

        return nameNType;

    }



    public String rType(String name, int[] line)
    {
        
        int Rm = Helper.bitParser(line, 11,5);
        int shamt = Helper.bitParser(line, 16, 6);
        int Rn = Helper.bitParser(line, 22, 5);
        int Rd = Helper.bitParser(line, 27, 5);

        if(name.equals("PRNT"))
            return name + " X" + Rd;
        else if(name.equals("PRNL") || name.equals("DUMP") || name.equals("HALT"))
            return name;
        else if(name.equals("BR"))
            return name + " X" + Rn;
        else if(name.subSequence(0, 2).equals("LS"))
            return name + " X" + Rd + ", X" + Rn + ", #" + shamt;
        else
            return name + " X" + Rd + ", X" + Rn + ", X" + Rm;

    }

    public String iType(String name, int[] line)
    {
        int imm = Helper.bitParser(line, 10, 12);
        imm = Helper.convertToSigned(imm, 12);
        int Rn = Helper.bitParser(line, 22, 5);
        int Rd = Helper.bitParser(line, 27, 5);

        return name + " X" + Rd + ", X" + Rn + ", #" +imm;
    }

    public String dType(String name, int[] line)
    {
        int DT_addr = Helper.bitParser(line, 11, 9);
        int Rn = Helper.bitParser(line, 22, 5);
        int Rt = Helper.bitParser(line, 27, 5);

        return name + " X" + Rt + ", [X" + Rn + ", #" + DT_addr + "]";
    }

    public String bType(String name, int[] line, int currLine)
    {
        int lineOffset = Helper.bitParser(line, 6, 26);

        //convert brOffset to signedInt
        lineOffset = Helper.convertToSigned(lineOffset, 26);
        int labelLine = currLine + lineOffset;


        String labelName = labeler(labelLine);
        

        return "B " + labelName;
    }


    public String cbType(String name, int[] line, int currLine)
    {
        int lineOffset = Helper.bitParser(line, 8, 19);
        int Rt = Helper.bitParser(line, 27, 5);


        //convert brOffset to signedInt
        lineOffset = Helper.convertToSigned(lineOffset, 19);

        int labelLine = currLine + lineOffset;
        String labelName = labeler(labelLine);
        
        if(name.substring(name.length()-2).equals("ND"))
        {
            name = name.substring(0,2);
            name += Helper.getBranchCondition(Rt);            
        }
        else if(name.substring(name.length()-1).equals("Z"))
            name += " X" + Rt + ",";
        

        return name + " " + labelName;
    }



    public String labeler(int labelLine)
    {
        String labelName = "NONE";
        //check if labelLine already exists
        //if so, get label name from hashValue
        //if not, add to labelHash <label line, label name>
        if(labelHash.containsKey(labelLine))
            labelName = labelHash.get(labelLine);
        else   
        {
            String nextLabelName = "label_" + labelHash.size();
            labelHash.put(labelLine, nextLabelName);
            labelName = nextLabelName;
        }

        return labelName;
    }

}





//--------------------------------------------HELPER----------------------------------------------------------

class Helper
{
    public static byte[] readInputFromFilename(String f) throws IOException
    {
        File file = new File(f);
        byte[] binaryInput = new byte[(int)file.length()];
        FileInputStream fStream = new FileInputStream(file);
        fStream.read(binaryInput);
        fStream.close();
        
        if(binaryInput.length%4 != 0) 
        {
            System.out.println("Input # of bytes not divisible by 4,, incorrect line size");
        }

        return binaryInput;
    }

    public static void fillOpcodeHash(HashMap<Integer, String> hash)
    {
        hash.put(1112, "ADDr");
        hash.put(580, "ADDIi");
        hash.put(2046, "DUMPr");
        hash.put(2045, "PRNTr");
        hash.put(2044, "PRNLr");
        hash.put(5,"Bb");
        hash.put(37,"BLb");
        hash.put(1880,"SUBSr");
        hash.put(1624,"SUBr");
        hash.put(84,"B.CONDcb");
        hash.put(181,"CBNZcb");
        hash.put(180,"CBZcb");
        hash.put(37,"BLb");
        hash.put(1616,"EORr");
        hash.put(840,"EORIi");
        hash.put(1360,"ORRr");
        hash.put(712,"ORRIi");
        hash.put(1624,"SUBr");
        hash.put(836,"SUBIi");
        hash.put(964,"SUBISi");
        hash.put(1240,"MULr");
        hash.put(1104,"ANDr");
        hash.put(584,"ANDIi");       
        hash.put(1986,"LDURd");        
        hash.put(1984,"STURd");        
        hash.put(2047,"HALTr");        
        hash.put(1712,"BRr");        
        hash.put(1691,"LSLr");        
        hash.put(1690,"LSRr");        

    }
 


    public static int[] getFourByteLine(int[] fullArr, int firstInd)
    {
        int[] four = new int[4];
        four[0] = fullArr[firstInd + 0];
        four[1] = fullArr[firstInd + 1];
        four[2] = fullArr[firstInd + 2];
        four[3] = fullArr[firstInd + 3];

        return four;
    }



    public static int[] byteArrayToInt(byte[] byteArr)
    {
        int[] out = new int[byteArr.length];

        for(int i = 0; i<out.length; i++)
        {
            if(byteArr[i]<0)
            {
                int temp = byteArr[i]+1;
                temp = 255 + temp;
                out[i] = temp;
            }
            else
                out[i] = (int)byteArr[i];
        }

        return out;
    }


    public static int bitParser(int[] fourBytes, int startInd, int length)
    {
        
        long full32 = 0;

        full32 += (long)fourBytes[3];
        full32 += ((long)fourBytes[2]) << 8;
        full32 += ((long)fourBytes[1]) << 16;
        full32 += ((long)fourBytes[0]) << 24;

        int rShamt = 32-(startInd + length);
        int leftMod = 1<<length;

        full32 >>= rShamt;//cut off right digits
        full32 %= leftMod;//cut off left digits


        return (int)full32;

    }


    public static int convertToSigned(int in, int numBits)
    {

        if(in > (1<<(numBits-1)))
            return in - (1<<numBits);
        else
            return in;

    }


    public static String getBranchCondition(int Rt)
    {
        String condition = "No_Condition";

            if(Rt == 0)
                condition = "EQ";
            if(Rt == 1)
                condition = "NE";
            if(Rt == 2)
                condition = "HS";
            if(Rt == 3)
                condition = "LO";
            if(Rt == 4)
                condition = "MI";
            if(Rt == 5)
                condition = "PL";
            if(Rt == 6)
                condition = "VS";
            if(Rt == 7)
                condition = "VC";
            if(Rt == 8)
                condition = "HI";
            if(Rt == 9)
                condition = "LS";
            if(Rt == 10)
                condition = "GE";
            if(Rt == 11)
                condition = "LT";
            if(Rt == 12)
                condition = "GT";
            if(Rt == 13)
                condition = "LE";
            if(Rt == 14)
                condition = "condOutOfBounds-14";
            if(Rt == 15)
                condition = "condOutOfBounds-15";

        return condition;
    }

}



