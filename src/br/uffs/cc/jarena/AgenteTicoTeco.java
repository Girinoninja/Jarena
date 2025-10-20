//Alunas: Ana Carolina Ceni e Ketlin Gonzaga
//Agente tico teco



package br.uffs.cc.jarena;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

public class AgenteTicoTeco extends Agente {

    private boolean emPerigo;
    private ArrayList<Point> pontosDeEnergiaConhecidos;
    private HashMap<String, Point> aliadosConhecidos;
    
    // CONSTANTES DE ESTRATÉGIA
    private static final int ENERGIA_CACADOR = 800;
    private static final int ENERGIA_EXPLORADOR = 500;
    private static final int ENERGIA_FUGA = 250; 
    private static final int ENERGIA_CRITICA = 100; // Nível de energia para parar totalmente
    private static final int TEMPO_MAXIMO_NO_COGUMELO = 100; // Turnos

    // LÓGICA INTERNA
    private String meuPapel;
    private int contadorTurnos;
    private Point objetivoAtual;
   private int tempoNoObjetivo;

// CONTROLE DE COGUMELOS
private Point cogumeloAtual = null;
private int tempoNoCogumelo;
private boolean deveProcurarNovoCogumelo;

private int energiaAnterior;
private ArrayList<Point> cogumelosEsgotados;
private int tempoSemEnergia = 0;
private final int LIMITE_TEMPO_SEM_ENERGIA = 10;

    public AgenteTicoTeco(Integer x, Integer y, Integer energia) {
        super(x, y, energia);
        setDirecao(geraDirecaoAleatoria());
        this.emPerigo = false;

        // Inicializa estruturas de dados
        this.pontosDeEnergiaConhecidos = new ArrayList<>();
        this.aliadosConhecidos = new HashMap<>();
        this.cogumelosEsgotados = new ArrayList<>(); // inicializa lista de cogumelos esgotados
        
        // Inicializa controle de cogumelos
        this.cogumeloAtual = null;
        this.tempoNoCogumelo = 0;
        this.deveProcurarNovoCogumelo = true; // Começa procurando
        
        // Estado inicial
        this.meuPapel = "EXPLORADOR";
        this.contadorTurnos = 0;
        this.energiaAnterior = getEnergia(); // registra energia inicial
    }

   
    @Override
    public void pensa() {
        contadorTurnos++;
        energiaAnterior = getEnergia(); // Atualiza energia anterior no início do turno
        atualizarPapel();

        // 1. VERIFICAÇÃO DE EMERGÊNCIA
        if (getEnergia() < ENERGIA_FUGA) {
            if (!emPerigo) {
                emPerigo = true;
                estrategiaFuga();
            }
            return;
        } else {
            emPerigo = false;
        }

        Point cogumeloMaisProximo = encontrarCogumeloMaisProximo();
if (cogumeloMaisProximo != null && calcularDistancia(cogumeloMaisProximo) < 20) {
    para();
    cogumeloAtual = cogumeloMaisProximo;
    tempoNoCogumelo++;
    return; // não faz mais nada neste turno
} atualizarPapel();

        cogumeloMaisProximo = encontrarCogumeloMaisProximo();

// Se está muito perto de um cogumelo → para e coleta energia
if (cogumeloMaisProximo != null && calcularDistancia(cogumeloMaisProximo) < 25) {
    para();
    cogumeloAtual = cogumeloMaisProximo;
    tempoSemEnergia = 0;
    enviaMensagem("COLETANDO:" + cogumeloMaisProximo.x + "," + cogumeloMaisProximo.y);
    return;
}

// Se estava em um cogumelo mas não está mais ganhando energia → tenta voltar
if (cogumeloAtual != null) {
    tempoSemEnergia++;

    // Se faz pouco tempo que perdeu o cogumelo, tenta voltar até ele
    if (tempoSemEnergia < LIMITE_TEMPO_SEM_ENERGIA) {
        moveEmDirecao(cogumeloAtual.x, cogumeloAtual.y);
        enviaMensagem("RECUPERANDO:" + cogumeloAtual.x + "," + cogumeloAtual.y);
        return;
    } else {
        // Desiste do cogumelo e volta à exploração normal
        cogumeloAtual = null;
        tempoSemEnergia = 0;
    }
}

        
        if (cogumeloAtual != null && isCogumeloAtualEsgotado()) {
            cogumelosEsgotados.add(cogumeloAtual);
            pontosDeEnergiaConhecidos.remove(cogumeloAtual);
            cogumeloAtual = null;
            objetivoAtual = null;
            deveProcurarNovoCogumelo = true;
        }

        // 2. LÓGICA DE PERMANÊNCIA NO COGUMELO (ESTRATÉGIA PRINCIPAL)
        if (cogumeloAtual != null) {
            tempoNoCogumelo++;
            // Verifica se um dos critérios para sair foi atingido
            if (deveSairDoCogumeloAtual()) {
                cogumeloAtual = null; // Libera o cogumelo e procura um novo objetivo no próximo turno
                objetivoAtual = null;
            } else {
                // Se não precisa sair, a regra é FICAR PARADO COLETANDO ENERGIA.
                // Se por acaso se afastou, volta para o centro do cogumelo.
                if (calcularDistancia(cogumeloAtual) > 15) {
                    moveEmDirecao(cogumeloAtual.x, cogumeloAtual.y);
                } else {
                    para(); 
                }
                return; // Ação do turno está decidida: ficar no cogumelo.
            }
        }

        // 3. LÓGICA DE PERMANÊNCIA NO COGUMELO
        if (cogumeloAtual != null) {
            tempoNoCogumelo++;
            
            // Verifica se deve sair do cogumelo atual
            if (deveSairDoCogumeloAtual()) {
                cogumeloAtual = null;
                objetivoAtual = null;
                deveProcurarNovoCogumelo = true;
            } else {
                // Fica no cogumelo - movimento mínimo
                if (calcularDistancia(cogumeloAtual) > 15) {
                    moveEmDirecao(cogumeloAtual.x, cogumeloAtual.y);
                } else {
                    para(); // Economiza energia
                }
                return; // Não faz mais nada neste turno
            }
        }

        // 4. DECISÃO ESTRATÉGICA E DEFINIÇÃO DE OBJETIVO
        if (objetivoAtual == null) {
            tomarDecisaoEstrategicaCogumelo();
            
            if (deveProcurarNovoCogumelo) {
                objetivoAtual = encontrarAreaComPoucosAliados();
            } else {
                Point melhorCogumelo = encontrarMelhorCogumeloParaFicar(calcularOcupacaoCogumelos());
                if (melhorCogumelo != null) {
                    objetivoAtual = melhorCogumelo;
                    cogumeloAtual = melhorCogumelo;
                    tempoNoCogumelo = 0;
                }
            }
        }

        // 5. EXECUÇÃO DO MOVIMENTO
        if (objetivoAtual != null) {
            executarMovimentoParaObjetivo();
        } else {
            
            encontrarCogumeloMaisProximo();
        }

      
        if (deveDividir()) {
            divide();
        }
        
        if (deveComunicar()) {
            comunicaStatus();
        }
        
        if (contadorTurnos % 100 == 0) {
            aliadosConhecidos.clear();
        }
    }




    private void tomarDecisaoEstrategicaCogumelo() {
        if (pontosDeEnergiaConhecidos.isEmpty()) {
            deveProcurarNovoCogumelo = true;
            return;
        }
        
        HashMap<Point, Integer> ocupacao = calcularOcupacaoCogumelos();
        double chanceBuscarNovo = calcularChanceBuscarNovoCogumelo(ocupacao);
        
        if (Math.random() < chanceBuscarNovo) {
            deveProcurarNovoCogumelo = true;
            cogumeloAtual = null;
        } else {
            deveProcurarNovoCogumelo = false;
            cogumeloAtual = encontrarMelhorCogumeloParaFicar(ocupacao);
        }
    }
     // private Point encontrarCogumeloMaisProximoDe(Point referencePoint) {
      //  Point maisProximo = null;
      //  double menorDistancia = Double.MAX_VALUE;
        
      //  for (Point cogumelo : pontosDeEnergiaConhecidos) {
      //      if (cogumelosEsgotados.contains(cogumelo)) continue;
            
       //     double dist = calcularDistancia(referencePoint, cogumelo);
       //     if (dist < menorDistancia) {
       //         menorDistancia = dist;
        //        maisProximo = cogumelo;
        //    }
       // }
      //  return maisProximo;
   // }


    private double calcularChanceBuscarNovoCogumelo(HashMap<Point, Integer> ocupacao) {
        double chance = 0.2; // Chance base de explorar
        if (pontosDeEnergiaConhecidos.size() <= 2) chance += 0.4;
        if (getEnergia() > 1200) chance += 0.2;
        
        boolean todosLotados = ocupacao.values().stream().allMatch(v -> v >= 2);
        if (todosLotados) chance += 0.3;
        
        return Math.min(chance, 0.9); // Garante no máximo 90% de chance
    }

    private HashMap<Point, Integer> calcularOcupacaoCogumelos() {
        HashMap<Point, Integer> ocupacao = new HashMap<>();
        pontosDeEnergiaConhecidos.forEach(c -> ocupacao.put(c, 0));
        
        for (Point aliadoPos : aliadosConhecidos.values()) {
            Point cogumeloProximo = encontrarCogumeloMaisProximoDe(aliadoPos);
            if (cogumeloProximo != null && aliadoPos.distance(cogumeloProximo) < 60) {
                ocupacao.put(cogumeloProximo, ocupacao.get(cogumeloProximo) + 1);
            }
        }
        return ocupacao;
    }

    private Point encontrarMelhorCogumeloParaFicar(HashMap<Point, Integer> ocupacao) {
        Point melhorCogumelo = null;
        double melhorPontuacao = -1;
        
        for (Point cogumelo : pontosDeEnergiaConhecidos) {
            int agentes = ocupacao.getOrDefault(cogumelo, 0);
            double distancia = calcularDistancia(cogumelo);
            
            // Pontuação: prefere cogumelos próximos e vazios.
            double pontuacao = (1000 / (distancia + 1)) - (agentes * 200);
            
            if (pontuacao > melhorPontuacao) {
                melhorPontuacao = pontuacao;
                melhorCogumelo = cogumelo;
            }
        }
        return melhorCogumelo;
    }

    private boolean deveSairDoCogumeloAtual() {
        if (tempoNoCogumelo > TEMPO_MAXIMO_NO_COGUMELO) return true;
        if (getEnergia() > 1500) return true; // Com energia cheia, melhor explorar
        
        int agentesProximos = contarAliadosProximos(cogumeloAtual);
        if (agentesProximos >= 3) return true; // Evita congestionamento
        
        return false;
    }
    

    private void estrategiaFuga() {
        // PARANDO QUANDO A ENERGIA ESTIVER ACABANDO
        if (getEnergia() < ENERGIA_CRITICA) {
            para(); // Para totalmente para conservar o resto da energia.
            enviaMensagem("SOCORRO_PARADO:" + getX() + "," + getY());
            return;
        }
        
        Point refugio = encontrarCogumeloMaisProximo();
        if (refugio != null) {
            objetivoAtual = refugio;
            moveEmDirecao(refugio.x, refugio.y);
        } else {
            // Se não conhece nenhum cogumelo, foge para uma direção aleatória
            setDirecao(geraDirecaoAleatoria());
        }
        enviaMensagem("SOCORRO:" + getX() + "," + getY());
    }

    /**
     * Retorna o cogumelo conhecido mais próximo da posição atual,
     * ignorando cogumelos marcados como esgotados.
     */
    private Point encontrarCogumeloMaisProximo() {
        if (pontosDeEnergiaConhecidos == null || pontosDeEnergiaConhecidos.isEmpty()) {
            return null;
        }
        Point maisProximo = null;
        double menorDistancia = Double.MAX_VALUE;
        for (Point cogumelo : pontosDeEnergiaConhecidos) {
            if (cogumelosEsgotados != null && cogumelosEsgotados.contains(cogumelo)) {
                continue;
            }
            double dist = calcularDistancia(cogumelo);
            if (dist < menorDistancia) {
                menorDistancia = dist;
                maisProximo = cogumelo;
            }
        }
        return maisProximo;
    }

 
    
    private Point encontrarAreaComPoucosAliados() {
        // Gera um ponto aleatório no mapa para explorar
        int x = (int)(Math.random() * Constants.LARGURA_MAPA);
        int y = (int)(Math.random() * Constants.ALTURA_MAPA);
        return new Point(x, y);
    }
    
    private int contarAliadosProximos(Point ponto) {
        int count = 0;
        for (Point aliado : aliadosConhecidos.values()) {
            if (ponto.distance(aliado) < 80) {
                count++;
            }
        }
        return count;
    }
    
    private double calcularDistancia(Point p) {
        return p.distance(getX(), getY());
    }
    
private Point encontrarCogumeloMaisProximoDe(Point referencePoint) {
    Point maisProximo = null;
    double menorDistancia = Double.MAX_VALUE;
    if (pontosDeEnergiaConhecidos == null || pontosDeEnergiaConhecidos.isEmpty()) {
        return null;
    }
    for (Point cogumelo : pontosDeEnergiaConhecidos) {
        // Ignora cogumelos já marcados como esgotados
        if (cogumelosEsgotados != null && cogumelosEsgotados.contains(cogumelo)) {
            continue;
        }
        double dist = referencePoint.distance(cogumelo);
        if (dist < menorDistancia) {
            menorDistancia = dist;
            maisProximo = cogumelo;
        }
    }
    return maisProximo;
}


    private boolean isCogumeloAtualEsgotado() {
        if (cogumeloAtual == null) return false;
        
        boolean pertoSuficiente = calcularDistancia(cogumeloAtual) < 40;
        boolean energiaNaoAumentou = getEnergia() <= energiaAnterior;
        boolean tempoSuficienteNoCogumelo = tempoNoCogumelo > 10; // Evita falsos positivos

        return pertoSuficiente && energiaNaoAumentou && tempoSuficienteNoCogumelo;
    }


    private boolean deveDividir() {
        if (!podeDividir() || getEnergia() < 1200) return false;
        // Só divide se estiver seguro em um cogumelo
        return cogumeloAtual != null && calcularDistancia(cogumeloAtual) < 50;
    }

    

    // duplicate tomarDecisaoEstrategicaCogumelo() removed; using the earlier implementation above

    private void adicionaPontoDeEnergiaConhecido(Point novoPonto) {
        if (!pontosDeEnergiaConhecidos.contains(novoPonto)) {
            pontosDeEnergiaConhecidos.add(novoPonto);
        }
    }

    private void atualizarPapel() {
        if (getEnergia() > ENERGIA_CACADOR) meuPapel = "CACADOR";
        else if (getEnergia() > ENERGIA_EXPLORADOR) meuPapel = "EXPLORADOR";
        else meuPapel = "SUPORTE";
    }

    
    private void comunicaStatus() {
        enviaMensagem("POS:" + getX() + "," + getY() + ":" + meuPapel);
        
        if (Math.random() < 0.1 && !pontosDeEnergiaConhecidos.isEmpty()) {
            Point cogumelo = pontosDeEnergiaConhecidos.get((int)(Math.random() * pontosDeEnergiaConhecidos.size()));
            enviaMensagem("COGUMELO:" + cogumelo.x + "," + cogumelo.y);
        }
    }
    
    private void moveEmDirecao(int x, int y) {
            int diffX = x - getX();
            int diffY = y - getY();
            // Prioriza o eixo com maior diferença para definir a direção
            if (Math.abs(diffX) > Math.abs(diffY)) {
                setDirecao(diffX > 0 ? DIREITA : ESQUERDA);
            } else {
                // Assume constantes CIMA/BAIXO existem no Agente base
                setDirecao(diffY > 0 ? BAIXO : CIMA);
            }
        }
    
        private boolean deveComunicar() {
            // Comunica periodicamente ou quando estiver com energia baixa.
            // Ajuste a frequência conforme necessário; aqui comunicamos a cada 5 turnos
            // ou sempre que a energia estiver abaixo do limiar de explorador.
            return (contadorTurnos % 5 == 0) || (getEnergia() < ENERGIA_EXPLORADOR);
        }
    
        private void executarMovimentoParaObjetivo() {
            if (objetivoAtual == null) return;
    
            double distancia = calcularDistancia(objetivoAtual);
    
            // Se já está suficientemente próximo, considera o objetivo alcançado
            if (distancia <= 6) {
                para();
                objetivoAtual = null;
                tempoNoObjetivo = 0;
                return;
            }
    
            // Move na direção do objetivo (seta a direção; o motor do ambiente executa o movimento)
            moveEmDirecao(objetivoAtual.x, objetivoAtual.y);
        }

    @Override
    public void recebeuMensagem(String msg) {
        String[] partes = msg.split(":");
        if (partes.length < 2) return;
        String comando = partes[0];
        
        try {
            if (comando.equals("COGUMELO")) {
                int x = Integer.parseInt(partes[1].split(",")[0]);
                int y = Integer.parseInt(partes[1].split(",")[1]);
                adicionaPontoDeEnergiaConhecido(new Point(x, y));
            } else if (comando.equals("POS") && partes.length >= 3) {
                int xAliado = Integer.parseInt(partes[1].split(",")[0]);
                int yAliado = Integer.parseInt(partes[1].split(",")[1]);
                // Usa a posição do aliado como chave única
                aliadosConhecidos.put(partes[1], new Point(xAliado, yAliado));
            } else if (comando.startsWith("SOCORRO") && meuPapel.equals("CACADOR")) {
                 int xSocorro = Integer.parseInt(partes[1].split(",")[0]);
                 int ySocorro = Integer.parseInt(partes[1].split(",")[1]);
                 objetivoAtual = new Point(xSocorro, ySocorro);
            }
        } catch (Exception e) {
            // Ignora mensagens mal formatadas
        }
    }

    @Override
    public void recebeuEnergia() {
        Point local = new Point(getX(), getY());
        adicionaPontoDeEnergiaConhecido(local);
        // Não precisa enviar mensagem aqui, pois o fato de estar recebendo
        // energia já é um sinal para si mesmo. A comunicação periódica cuida do resto.
    }

    @Override
    public void tomouDano(int energiaRestanteInimigo) {
    if (getEnergia() > 900) {
        // Está forte: luta
        para(); // Mantém posição e luta
        enviaMensagem("LUTANDO:" + getX() + "," + getY());
    } else {
        // Está fraco: foge
        estrategiaFuga();
        enviaMensagem("FUGINDO:" + getX() + "," + getY());
    }
}

    @Override
    public void ganhouCombate() {
        objetivoAtual = null; // Após a vitória, reavalia o que fazer
    }
    
    @Override
    public String getEquipe() {
        return "GirinoEninja";
    }
}