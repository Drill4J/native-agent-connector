#include <iostream>
#include <libdrilldotnet_api.h>



void xx(char* x1, char* x2){
    printf("destination: %s\n", x1);
    printf("message: %s\n", x2);
}

int main() {

    libdrilldotnet_ExportedSymbols *ptr = libdrilldotnet_symbols();
    void (*fun)(char *, char *) = xx;
    void *function = (void *)(fun);
    initialize_agent("mysuperAgent", "localhost:8090", "1.0.0", "group", "fail",function);
    printf("1");

    printf("2");
    sendMessage("10", "ahahah");

    

    while(true) {

    }
    return 0;
}
