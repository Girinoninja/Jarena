/**
 * Um exemplo de agente que anda aleatoriamente na arena. Esse agente pode ser usado como base
 * para a criação de um agente mais esperto. Para mais informações sobre métodos que podem
 * ser utilizados, veja a classe Agente.java.
 * 
 * Fernando Bevilacqua <fernando.bevilacqua@uffs.edu.br>
 */

package br.uffs.cc.jarena;

public class AgenteTicoTeco extends Agente
{
	private boolean procurandoCogumelo;
    private int ultimaDirecao;
    private int energiaParaFugir;
    private boolean emPerigo;
    
    public AgenteTicoTeco(Integer x, Integer y, Integer energia) {
        super(x, y, energia);
        setDirecao(geraDirecaoAleatoria());
        procurandoCogumelo = true;
        energiaParaFugir = 300; // Foge se tiver menos que 300 de energia
        emPerigo = false;
    }
    
    @Override
    public void pensa() {
        // Estratégia principal: buscar cogumelos
        if (procurandoCogumelo) {
            buscaCogumelos();
        }
        
        // Verifica se está em perigo (pouca energia)
        if (getEnergia() < energiaParaFugir) {
            emPerigo = true;
            estrategiaFuga();
        } else {
            emPerigo = false;
        }
        
        // Divisão estratégica
        if (podeDividir() && getEnergia() >= 1200 && !emPerigo) {
            divide();
        }
        
        // Comunicação periódica
        if (Math.random() < 0.02) { // 2% de chance por turno
            comunicaPosicao();
        }
    }
    
    private void buscaCogumelos() {
        // Se não pode mover na direção atual, muda aleatoriamente
        if (!podeMoverPara(getDirecao())) {
            int novaDirecao = geraDirecaoAleatoria();
            setDirecao(novaDirecao);
            ultimaDirecao = novaDirecao;
        }
        
        // Movimento exploratório com ocasionais mudanças de direção
        if (Math.random() < 0.1) { // 10% de chance de mudar direção
            setDirecao(geraDirecaoAleatoria());
        }
    }
    
    private void estrategiaFuga() {
        // Para conservar energia
        para();
        
        // Se estiver muito fraco, foge em direção oposta
        if (getEnergia() < 150) {
            setDirecao(direcaoOposta(ultimaDirecao));
        }
        
        // Comunica que está em perigo
        enviaMensagem("PERIGO:" + getX() + "," + getY() + ":" + getEnergia());
    }
    
    private int direcaoOposta(int direcao) {
        switch (direcao) {
            case DIREITA: return ESQUERDA;
            case ESQUERDA: return DIREITA;
            case CIMA: return BAIXO;
            case BAIXO: return CIMA;
            default: return geraDirecaoAleatoria();
        }
    }
    
    private void comunicaPosicao() {
        String mensagem = "POS:" + getX() + "," + getY();
        if (getEnergia() > 800) {
            mensagem += ":FORTE";
        } else if (getEnergia() > 400) {
            mensagem += ":MEDIO";
        } else {
            mensagem += ":FRACO";
        }
        enviaMensagem(mensagem);
    }
    
    @Override
    public void recebeuEnergia() {
        // Achou um cogumelo! Comunica para outros agentes
        procurandoCogumelo = false;
        enviaMensagem("COGUMELO:" + getX() + "," + getY());
        
        // Continua procurando após um tempo
        new java.util.Timer().schedule( 
            new java.util.TimerTask() {
                @Override
                public void run() {
                    procurandoCogumelo = true;
                }
            }, 
            5000 // 5 segundos
        );
    }
    
    @Override
    public void tomouDano(int energiaRestanteInimigo) {
        // Avalia se deve fugir ou continuar lutando
        if (getEnergia() < energiaRestanteInimigo || emPerigo) {
            // Inimigo está mais forte, foge!
            setDirecao(direcaoOposta(getDirecao()));
            enviaMensagem("FUGA:" + getX() + "," + getY());
        } else {
            // Estamos mais fortes, continua atacando
            enviaMensagem("COMBATE:" + getX() + "," + getY());
        }
    }
    
    @Override
    public void ganhouCombate() {
        // Comemora a vitória e comunica
        enviaMensagem("VITORIA:" + getX() + "," + getY());
        procurandoCogumelo = true; // Volta a procurar cogumelos
    }
    
    @Override
    public void recebeuMensagem(String msg) {
        // Sistema de comunicação entre agentes
        if (msg.startsWith("POS:")) {
            // Outro agente comunicou posição
            String[] partes = msg.split(":");
            String[] coordenadas = partes[1].split(",");
            int xAliado = Integer.parseInt(coordenadas[0]);
            int yAliado = Integer.parseInt(coordenadas[1]);
            String status = partes[2];
            
            // Estratégia: se estiver fraco, tenta se agrupar com aliados fortes
            if (emPerigo && status.equals("FORTE")) {
                moveEmDirecao(xAliado, yAliado);
            }
            
        } else if (msg.startsWith("COGUMELO:")) {
            // Aliado encontrou cogumelo - vamos lá!
            String[] coordenadas = msg.split(":")[1].split(",");
            int xCogumelo = Integer.parseInt(coordenadas[0]);
            int yCogumelo = Integer.parseInt(coordenadas[1]);
            
            moveEmDirecao(xCogumelo, yCogumelo);
            
        } else if (msg.startsWith("PERIGO:")) {
            // Aliado em perigo - vamos ajudar se estivermos fortes
            if (getEnergia() > 600) {
                String[] partes = msg.split(":");
                String[] coordenadas = partes[1].split(",");
                int xAliado = Integer.parseInt(coordenadas[0]);
                int yAliado = Integer.parseInt(coordenadas[1]);
                
                moveEmDirecao(xAliado, yAliado);
                enviaMensagem("AJUDA_CHEGANDO:" + getX() + "," + getY());
            }
        }
    }
    
    private void moveEmDirecao(int xDestino, int yDestino) {
        int diffX = xDestino - getX();
        int diffY = yDestino - getY();
        
        if (Math.abs(diffX) > Math.abs(diffY)) {
            // Move mais na horizontal
            setDirecao(diffX > 0 ? DIREITA : ESQUERDA);
        } else {
            // Move mais na vertical
            setDirecao(diffY > 0 ? BAIXO : CIMA);
        }
    }
    
    @Override
    public String getEquipe() {
        return "GirinoEninja";
    }
}