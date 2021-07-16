import java.io.Serializable;

public class IncidenceServiceUtil implements Serializable {
    private final static long serialVersionUID = 1L;

    public static String removeMaskFromDocument (String document) {
        return document.replace(".", "").replace("-", "").replace("/", "").trim();
    }

    public static boolean isCpfCnpjValid(String document) {

        isValidForReceitaFederal(document);

        return isCpfValid(document) || isCnpjValid(document);
    }

    /**
     * Não é considerado válido para a Receita Federal, documentos onde todos os dígitos são sequênciais, como:
     * CPF 000.000.000-00
     * CNPJ 00.000.000/0000-00
     *
     * O algoritmo identifica se algum dígito é diferente o primeiro dígito. Se sim, retorna ou caso não, vai percorrer e lança uma IncidenceServiceBadRequestException.
     *
     * @author Victor Pinho
     * @since 15/07/2021
     * @param document CPF ou CNPJ sem máscara
     * @throws IncidenceServiceBadRequestException
     */
    private static void isValidForReceitaFederal (String document) throws IncidenceServiceBadRequestException {
        String [] sequences = document.split("");
        boolean isEqual;
        int sizeOfCpf = 11;
        int sizeOfCnpj = 14;

        if (document.length() == sizeOfCpf || document.length() == sizeOfCnpj) {
            do {
                for (String value : sequences) {
                    isEqual = value.equals(sequences[0]);
                    if (isEqual == false) return;
                }
                break;
            }
            while (isEqual);
        }
        throw new IncidenceServiceBadRequestException("The number entered is not a valid Cpf or Cnpj for consultation");
    }

    /**
     * O algoritmo verifica se o CPF está de acordo com a Receita Federal.
     * O algoritmo é feito em duas partes. Na primeira verificação, é levado em consideração os 9 primeiros dígitos e na segunda os 10 primeiros dígitos.
     *
     * No primeiro parte, cada dígito vai ser multiplicado por um dos multiplicadores da sequência 10, 9, 8, 7, 6, 5, 4, 3, 2, ficando:
     * Primeiro dígito do bloco * 10 | Segundo dígito do bloco * 9 | Terceiro dígito do bloco * 8 | Quarto dígito do bloco * 7 | Quinto dígito do bloco * 6 | Sexto dígito do bloco * 5 | Sétimo dígito do bloco * 4 | Oitavo dígito do bloco * 3 | Nono dígito do bloco * 2
     *
     * Somamos todos os valores obtidos na multiplicação e dividimos por 11.
     * Analisamos o resto. Se for menor que 2, o décimo terceiro dígito é 0. Mas se for maior ou igual 2, o décimo terceiro dígito é o resultado de 11 menos ('-')  o resto da divisão.
     * Agora é só verificar se o valor é igual ao ao primeiro dígito verificador (DV) informado no documento.
     *
     * A segunda parte é a mesma que a primeira, mas desta vez é levado em consideração os 10 primeiros dígitos e usaremos a sequência de multiplicadores :
     * No primeiro parte, cada dígito vai ser multiplicado por um dos multiplicadores da sequência 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, ficando:
     * Primeiro dígito do bloco * 11 | Segundo dígito do bloco * 10 | Terceiro dígito do bloco * 9 | Quarto dígito do bloco * 8 | Quinto dígito do bloco * 7 | Sexto dígito do bloco * 6 | Sétimo dígito do bloco * 5 | Oitavo dígito do bloco * 4 | Nono dígito do bloco * 3 | Décimo dígito do bloco * 2
     *
     * Somamos todos os valores obtidos na multiplicação e dividimos por 11.
     *Analisamos o resto. Se for menor que 2, o décimo terceiro dígito é 0. Mas se for maior ou igual 2, o décimo terceiro dígito é o resultado de 11 menos ('-')  o resto da divisão.
     *Agora é só verificar se o valor é igual ao ao segundo dígito verificador (DV) informado no documento.
     *
     * @author Victor Pinho
     * @since 15/07/2021
     * @param document Número de CNPJ para ser verificado.
     * @return Se válido true ou false para inválido
     */
    private static boolean isCpfValid(String document) {
        int validCpfSize = 11, rest, sun = 0, countFirstDigitVerify = 10, countSecondDigitVerify = 11, firstDigitVerify, secondDigitVerify;
        Boolean result = false;
        String [] split = document.split("");

        if (document.length() == validCpfSize) {
            for (int i = 0; i <= 8; i++) {
                sun += Integer.parseInt(split[i]) * countFirstDigitVerify--;
            }
            rest = (sun % document.length());
            firstDigitVerify = rest < 2 ? 0 :  11 - rest;

            if (firstDigitVerify !=  Integer.parseInt(split[split.length - 2])) return false;

            sun = 0;
            for (int j = 0; j <= 9; j++) {
                sun += Integer.parseInt(split[j]) * countSecondDigitVerify--;
            }
            rest = (sun % document.length());
            secondDigitVerify = rest < 2 ? 0 :  11 - rest;

            result = secondDigitVerify ==  Integer.parseInt(split[split.length - 1]);
        }
        return result;
    }

    /**
     * O algoritmo verifica se o CNPJ está de acordo com a Receita Federal.
     * O algoritmo é feito em duas partes. Na primeira verificação, é levado em consideração os 12 primeiros dígitos e na segunda os 13 primeiros dígitos.
     * Dividimos os 12 primeiros dígitos em dois blocos, onde o primeiro bloco é composto pelos 4 primeiros dígitos e o segundo pelos 8 restantes.
     *
     * No primeiro bloco, cada dígito vai ser multiplicado por um dos multiplicadores da sequência 5, 4, 3, 2, ficando:
     * Primeiro dígito do bloco * 5 | Segundo dígito do bloco * 4 | Terceiro dígito do bloco * 3 | Quarto dígito do bloco * 2
     *
     * No segundo bloco, cada dígito vai ser multiplicado por um dos multiplicadores da sequência 9, 8, 7, 6, 5, 4, 3, 2 ficando:
     * Primeiro dígito do bloco * 9 | Segundo dígito do bloco * 8 | Terceiro dígito do bloco * 7 | Quarto dígito do bloco * 6 | Quinto dígito do bloco * 5 | Sexto dígito do bloco * 4 | Sétimo dígito do bloco * 3 | Oitavo dígito do bloco * 2
     *
     * Somamos todos os valores obtidos na multiplicação e dividimos por 11.
     * Analisamos o resto. Se for menor que 2, o décimo terceiro dígito é 0. Mas se for maior ou igual 2, o décimo terceiro dígito é o resultado de 11 menos ('-')  o resto da divisão.
     * Agora é só verificar sé o valor é igual ao informado no documento.
     *
     * A segunda parte é a mesma que a primeira, mas desta vez é levado em consideração os 13 primeiros dígitos e usaremos a sequência de multiplicadores :
     * Primeiro bloco 6, 5, 4, 3, 2
     * Segundo bloco 9, 8, 7, 6, 5, 4, 3, 2
     *
     * Agora só usar a mesma lógica para saber se o décimo quarto dígito é válido.
     *
     * @author Victor Pinho
     * @since 15/07/2021
     * @param document Número de CNPJ para ser verificado.
     * @return Se válido true ou false para inválido
     */
    private static boolean isCnpjValid (String document) {
        int validCnpjSize = 14, rest, sun = 0, countFirstBlock = 5, countSecondBlock = 9, firstDigitVerify, secondDigitVerify;
        Boolean result = false;
        String [] split = document.split("");

        if (document.length() == validCnpjSize) {
            for (int i = 0; i <= 3; i++) {
                sun += Integer.parseInt(split[i]) * countFirstBlock--;
            }
            for (int j = 4; j <= 11; j++) {
                sun += Integer.parseInt(split[j]) * countSecondBlock--;
            }
            rest = (sun % 11);
            firstDigitVerify = rest < 2 ? 0 :  11 - rest;

            if (firstDigitVerify !=  Integer.parseInt(split[split.length - 2])) return false;

            sun = 0; countFirstBlock = 6; countSecondBlock = 9;

            for (int l = 0; l <= 4; l++) {
                sun += Integer.parseInt(split[l]) * countFirstBlock--;
            }
            for (int m = 5; m <= 12; m++) {
                sun += Integer.parseInt(split[m]) * countSecondBlock--;
            }
            rest = (sun % 11);
            secondDigitVerify = rest < 2 ? 0 :  11 - rest;
            result = secondDigitVerify ==  Integer.parseInt(split[split.length - 1]);
        }
        return result;
    }

}