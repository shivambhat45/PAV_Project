class Test{

//    1. Write for different possibilities of if condition less than ,less than equal to ,not equal ,if equal too and check the working
//      2. Other Possible Cases

    static int fun1(int check){
        int x = 10;
        if(check < 100){
            x = x - 1;
        }
        else{
            x = x + 2;
        }
        return x+check;

    }

    static int fun2()
    {
        int i=0;
        while(i!=5)
        {
            i++;
        }
        return i;
    }

    static int fun3()
    {
        int i=0;
        while(i==5)
        {
            i++;
        }
        return i;
    }

    static int fun4()
    {
        int i=9;
        while(i>5)
        {
            i=i-1;
        }
        return i;
    }

    static int fun5()
    {
        int i=9;
        while(i<=5)
        {
            i=i-1;
        }
        return i;
    }

    static int fun6()
    {
        int i=1;
        while(i>0)
        {
            i=i+1;
        }

        return i;
    }

}









