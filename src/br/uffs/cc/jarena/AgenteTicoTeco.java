/**
 * AgenteTicoTeco com Estratégia de Enxame Avançada
 * * Este agente utiliza um sistema de papéis dinâmicos (Caçador, Suporte, Explorador)
 * para adaptar seu comportamento à situação da arena. Ele memoriza informações,
 * coopera com aliados e toma decisões táticas.
 */
package br.uffs.cc.jarena;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class AgenteTicoTeco extends Agente {
    // VARIÁVEIS DE ESTADO E MEMÓRIA
    private boolean emPerigo;
    private ArrayList<Point> pontosDeEnergiaConhecidos;
    private HashMap<String, Point> aliadosConhecidos;
    private ArrayList<Point> inimigosReportados;
    
    // CONSTANTES DE ESTRATÉGIA
    private static final int ENERGIA_CACADOR = 800;
    private static final int ENERGIA_SUPORTE = 500;
    private static final int ENERGIA_FUGA = 300;
    private static final int DISTANCIA_SEGURA_DIVISAO = 70;
    private static final int DISTANCIA_EVITAR_CONGESTIONAMENTO = 50;
    
    // VARIÁVEIS DE LÓGICA INTERNA
    private String meuPapel;
    private int contadorTurnos;
    private Point objetivoAtual;
    private int tempoNoObjetivo;

    public AgenteTicoTeco(Integer x, Integer y, Integer energia) {
        super(x, y, energia);
        setDirecao(geraDirecaoAleatoria());
        this.emPerigo = false;

        // Inicializa estruturas de dados
        this.pontosDeEnergiaConhecidos = new ArrayList<>();
        this.aliadosConhecidos = new HashMap<>();
        this.inimigosReportados = new ArrayList<>();
        
        // Define estado inicial
        this.contadorTurnos = 0;
        this.objetivoAtual = null;
        this.tempoNoObjetivo = 0;
        atualizarPapel(); // Define o papel inicial
    }
    
    @Override
    public void pensa() {
        contadorTurnos++;
        
        // 1. ATUALIZAÇÃO DE ESTADO E PAPEL
        atualizarPapel();
        
        // 2. AVALIAÇÃO DE EMERGÊNCIA (prioridade máxima)
        if (getEnergia() < ENERGIA_FUGA) {
            if (!emPerigo) {
                emPerigo = true;
                estrategiaFugaAvancada();
            }
            return; // Emergência tem prioridade total
        } else {
            emPerigo = false;
        }

        // 3. VERIFICAÇÃO DE OBJETIVO ATUAL
        if (objetivoAtual != null) {
            tempoNoObjetivo++;
            // Se está há muito tempo no mesmo objetivo ou já chegou perto, reconsidera
            if (tempoNoObjetivo > 50 || calcularDistancia(objetivoAtual) < 20) {
                objetivoAtual = null;
                tempoNoObjetivo = 0;
            }
        }

        // 4. DECISÃO ESTRATÉGICA BASEADA NO PAPEL
        switch (meuPapel) {
            case "EXPLORADOR":
                estrategiaExplorador();
                break;
            case "CACADOR":
                estrategiaCacador();
                break;
            case "SUPORTE":
                estrategiaSuporte();
                break;
            default:
                estrategiaExplorador();
        }

        // 5. DIVISÃO ESTRATÉGICA
        if (deveDividir()) {
            divide();
        }

        // 6. COMUNICAÇÃO (a comunicação agora é feita dentro das estratégias)
        
        // 7. LIMPEZA DE DADOS ANTIGOS
        if (contadorTurnos % 100 == 0) {
            // Inimigos reportados são informações voláteis
            inimigosReportados.clear();
        }
    }

    // ========== ESTRATÉGIAS ESPECÍFICAS POR PAPEL ==========

    private void estrategiaExplorador() {
        // Foco: encontrar novos cogumelos
        if (objetivoAtual == null) {
            objetivoAtual = encontrarAreaNaoExplorada();
        }
        
        if (objetivoAtual != null) {
            moveEmDirecao(objetivoAtual.x, objetivoAtual.y);
        } else {
            // Se não há áreas inexploradas, patrulha
            exploracaoSistematica();
        }
    }
    
    private void estrategiaCacador() {
        // Foco: eliminar inimigos e proteger áreas
        if (!inimigosReportados.isEmpty()) {
            objetivoAtual = encontrarInimigoMaisProximo();
            if (objetivoAtual != null) {
                moveEmDirecao(objetivoAtual.x, objetivoAtual.y);
                return;
            }
        }
        
        // Se não há inimigos, patrulha entre os cogumelos
        if (objetivoAtual == null) {
            objetivoAtual = encontrarPontoPatrulha();
        }
        
        if (objetivoAtual != null) {
            moveEmDirecao(objetivoAtual.x, objetivoAtual.y);
        } else {
            exploracaoSistematica();
        }
    }

    private void estrategiaSuporte() {
        // Foco: coletar energia e evitar congestionamento
        if (objetivoAtual == null) {
             Point cogumelo = encontrarCogumeloMaisProximo(true); // Procura um cogumelo não congestionado
             if (cogumelo != null) {
                 objetivoAtual = cogumelo;
             } else {
                 // Se todos estão congestionados, se afasta para uma área vazia
                 objetivoAtual = encontrarAreaNaoExplorada();
             }
        }

        if (objetivoAtual != null) {
            moveEmDirecao(objetivoAtual.x, objetivoAtual.y);
        } else {
            exploracaoSistematica();
        }
    }

    // ========== MÉTODOS DE LÓGICA E APOIO ==========

    private void atualizarPapel() {
        if (getEnergia() > ENERGIA_CACADOR && !emPerigo) {
            meuPapel = "CACADOR";
        } else if (getEnergia() > ENERGIA_SUPORTE && !emPerigo) {
            meuPapel = "SUPORTE";
        } else {
            meuPapel = "EXPLORADOR";
        }
    }

    private void estrategiaFugaAvancada() {
        para(); // Para imediatamente para economizar energia
        
        Point refugio = encontrarCogumeloMaisProximo(true); // Foge para cogumelo não congestionado
        if (refugio != null) {
            objetivoAtual = refugio;
            moveEmDirecao(objetivoAtual.x, objetivoAtual.y);
        } else {
            // Fuga aleatória, mas tentando se afastar dos inimigos
            setDirecao(geraDirecaoFuga());
        }
        enviaMensagem("SOCORRO:" + getX() + "," + getY() + ":" + getEnergia());
    }

    private int geraDirecaoFuga() {
        if (inimigosReportados.isEmpty()) {
            return geraDirecaoAleatoria();
        }
        // Calcula o ponto médio dos inimigos e foge na direção oposta
        Point centroideInimigo = calcularCentroide(inimigosReportados);
        int diffX = getX() - centroideInimigo.x;
        int diffY = getY() - centroideInimigo.y;
        
        if (Math.abs(diffX) > Math.abs(diffY)) {
            return diffX > 0 ? DIREITA : ESQUERDA;
        } else {
            return diffY > 0 ? BAIXO : CIMA;
        }
    }
    
    private boolean deveDividir() {
        if (!podeDividir()) return false;
        
        boolean pertoDeEnergia = isPertoDeEnergia();
        switch (meuPapel) {
            case "CACADOR":
                return getEnergia() >= 1200 && inimigosReportados.isEmpty() && pertoDeEnergia;
            case "SUPORTE":
                return getEnergia() >= 1100 && pertoDeEnergia;
            case "EXPLORADOR":
                 return getEnergia() >= 1000 && pontosDeEnergiaConhecidos.size() >= 1 && pertoDeEnergia;
            default:
                return false;
        }
    }

    // ========== MÉTODOS DE EVENTOS DA ARENA ==========

    @Override
    public void recebeuEnergia() {
        Point local = new Point(getX(), getY());
        adicionaPontoDeEnergiaConhecido(local);
        enviaMensagem("COGUMELO:" + getX() + "," + getY());
    }

    @Override
    public void tomouDano(int energiaRestanteInimigo) {
        enviaMensagem("INIMIGO_AVISTADO:" + getX() + "," + getY());
        if (getEnergia() < energiaRestanteInimigo * 1.2) { // Foge se o inimigo for um pouco mais forte
            estrategiaFugaAvancada();
        }
    }

    @Override
    public void ganhouCombate() {
        enviaMensagem("VITORIA:" + getX() + "," + getY());
        objetivoAtual = null; // Recalcula o objetivo após o combate
    }
    
    @Override
    public void recebeuMensagem(String msg) {
        String[] partes = msg.split(":");
        String comando = partes[0];
        
        try {
            switch (comando) {
                case "COGUMELO":
                    int x = Integer.parseInt(partes[1].split(",")[0]);
                    int y = Integer.parseInt(partes[1].split(",")[1]);
                    adicionaPontoDeEnergiaConhecido(new Point(x, y));
                    break;

                case "INIMIGO_AVISTADO":
                    int ix = Integer.parseInt(partes[1].split(",")[0]);
                    int iy = Integer.parseInt(partes[1].split(",")[1]);
                    adicionaInimigoReportado(new Point(ix, iy));
                    break;
                
                case "SOCORRO":
                    // Se for um Caçador ou Suporte, avalia ir ajudar
                    if (meuPapel.equals("CACADOR") || meuPapel.equals("SUPORTE")) {
                        int sx = Integer.parseInt(partes[1].split(",")[0]);
                        int sy = Integer.parseInt(partes[1].split(",")[1]);
                        objetivoAtual = new Point(sx, sy);
                        tempoNoObjetivo = 0;
                    }
                    break;
            }
        } catch (Exception e) {
            // Ignora mensagens mal formatadas
        }
    }
    
    // ========== FUNÇÕES AUXILIARES ==========
    
    private void moveEmDirecao(int xDestino, int yDestino) {
        int diffX = xDestino - getX();
        int diffY = yDestino - getY();
        if (Math.abs(diffX) > Math.abs(diffY)) {
            setDirecao(diffX > 0 ? DIREITA : ESQUERDA);
        } else {
            setDirecao(diffY > 0 ? BAIXO : CIMA);
        }
    }

    private void adicionaPontoDeEnergiaConhecido(Point p) {
        if (!pontosDeEnergiaConhecidos.contains(p)) {
            pontosDeEnergiaConhecidos.add(p);
        }
    }

    private void adicionaInimigoReportado(Point p) {
        if (!inimigosReportados.contains(p)) {
            inimigosReportados.add(p);
        }
    }
    
    private Point encontrarCogumeloMaisProximo(boolean evitarCongestionamento) {
        Point maisProximo = null;
        double menorDistancia = Double.MAX_VALUE;

        for (Point p : pontosDeEnergiaConhecidos) {
            if (evitarCongestionamento && estaCongestionado(p)) {
                continue; // Pula este ponto se estiver congestionado
            }
            double d = calcularDistancia(p);
            if (d < menorDistancia) {
                menorDistancia = d;
                maisProximo = p;
            }
        }
        return maisProximo;
    }

    private Point encontrarInimigoMaisProximo() {
        return encontrarPontoMaisProximo(inimigosReportados);
    }
    
    private Point encontrarPontoMaisProximo(ArrayList<Point> lista) {
        if (lista == null || lista.isEmpty()) return null;
        Point maisProximo = null;
        double menorDistancia = Double.MAX_VALUE;

        for (Point p : lista) {
            double d = calcularDistancia(p);
            if (d < menorDistancia) {
                menorDistancia = d;
                maisProximo = p;
            }
        }
        return maisProximo;
    }

    private boolean estaCongestionado(Point ponto) {
           if (objetivoAtual != null && objetivoAtual.equals(ponto)) {
            return true;
        }
        return false; 
    }

    private Point encontrarAreaNaoExplorada() {
        if (pontosDeEnergiaConhecidos.isEmpty()) {
            return new Point(Constants.LARGURA_MAPA / 2, Constants.ALTURA_MAPA / 2);
        }
        Point centro = calcularCentroide(pontosDeEnergiaConhecidos);
        return new Point(Constants.LARGURA_MAPA - centro.x, Constants.ALTURA_MAPA - centro.y);
    }
    
    private Point encontrarPontoPatrulha() {
        if (pontosDeEnergiaConhecidos.size() < 2) {
            return encontrarAreaNaoExplorada();
        }
        // Patrulha entre os dois primeiros cogumelos conhecidos
        Point p1 = pontosDeEnergiaConhecidos.get(0);
        Point p2 = pontosDeEnergiaConhecidos.get(1);
        return new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
    }
    
    private void exploracaoSistematica() {
        if (!podeMoverPara(getDirecao()) || contadorTurnos % 20 == 0) {
            setDirecao((getDirecao() % 4) + 1); // Vira 90 graus
        }
    }
    
    private double calcularDistancia(Point p) {
        return p.distance(getX(), getY());
    }
    
    private Point calcularCentroide(ArrayList<Point> lista) {
        if (lista.isEmpty()) return new Point(getX(), getY());
        int sumX = 0, sumY = 0;
        for (Point p : lista) {
            sumX += p.x;
            sumY += p.y;
        }
        return new Point(sumX / lista.size(), sumY / lista.size());
    }

    private boolean isPertoDeEnergia() {
        if (pontosDeEnergiaConhecidos.isEmpty()) return false;
        for (Point p : pontosDeEnergiaConhecidos) {
            if (calcularDistancia(p) < DISTANCIA_SEGURA_DIVISAO) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getEquipe() {
        return "GirinoEninja";
    }
}