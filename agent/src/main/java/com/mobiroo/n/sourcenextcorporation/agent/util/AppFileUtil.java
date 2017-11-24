package com.mobiroo.n.sourcenextcorporation.agent.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Pritam on 28/06/17.
 */

public class AppFileUtil {

    private String mFileName;

    public AppFileUtil(String filename){
        this.mFileName = filename;
    }

    /**
     * funciton to write into file
     * @param text
     * @return
     */
    public boolean writeFile(String text){
        boolean isSuccess = true;

        try {
            FileOutputStream fos = new FileOutputStream(mFileName);
            fos.write(text.getBytes());
            fos.close();
        } catch (IOException e) {
            isSuccess = false;
            e.printStackTrace();
        }

        return isSuccess;
    }

    /**
     * funciton to read from file
     * @return
     */
    public String readFile(){
        String content = null;

        try {
            FileInputStream fis = new FileInputStream(mFileName);
            DataInputStream in = new DataInputStream(fis);
            BufferedReader br =
                    new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                content = content + strLine;
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content;
    }


}
