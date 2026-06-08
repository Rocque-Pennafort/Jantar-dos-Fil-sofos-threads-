import java.util.concurrent.locks.Lock; 
import java.util.concurrent.atomic.AtomicBoolean; 

// Implementação da interface Runnable, permitindo que a classe descreva a tarefa executada por uma Thread
class Filosofo implements Runnable { 
    private final int id; // Identificador unívoco da thread
    private final Lock palitoEsquerdo; // Recurso compartilhado com a thread vizinha à esquerda
    private final Lock palitoDireito;  // Recurso compartilhado com a thread vizinha à direita
    private final AtomicBoolean continuar; // Monitoramento da condição de parada global

    public Filosofo(int id, Lock palitoEsquerdo, Lock palitoDireito, AtomicBoolean continuar) {
        this.id = id; 
        this.palitoEsquerdo = palitoEsquerdo; 
        this.palitoDireito = palitoDireito; 
        this.continuar = continuar; 
    }

    private void pensar() throws InterruptedException { 
        System.out.println("Filósofo " + id + " está pensando.");
        // Força a transição de estado da thread: RUNNABLE -> TIMED_WAITING
        Thread.sleep(1000); 
    }

    private void comer() throws InterruptedException { 
        System.out.println("Filósofo " + id + " está comendo."); 
        // Permanece em TIMED_WAITING enquanto retém os locks sob exclusão mútua
        Thread.sleep(1000); 
    }

    @Override
    public void run() { 
        try {
            // Execução cíclica concorrente mapeada pela flag de controle cooperativo
            while (continuar.get()) { 
                pensar(); 

                // ESTRATÉGIA DE PREVENÇÃO DE DEADLOCK (Quebra de Simetria Dinâmica):
                // Se todas as threads executassem a mesma sequência de aquisição, ocorreria Deadlock.
                // Condicionamos a ordem de requisição de travas à paridade do identificador da thread.
                if (id % 2 == 0) {
                    // Threads com identificação PAR requisitam primeiro o palito direito.
                    // Caso o recurso esteja retido por outra thread, esta thread muda instantaneamente para BLOCKED.
                    palitoDireito.lock(); 
                    System.out.println("Filósofo " + id + " pegou o palito direito."); 
                    
                    palitoEsquerdo.lock(); 
                    System.out.println("Filósofo " + id + " pegou o palito esquerdo."); 

                    try {
                        comer(); 
                    } finally {
                        // Desbloqueio explícito em bloco 'finally' para impedir Starvation permanente do sistema
                        palitoEsquerdo.unlock(); 
                        System.out.println("Filósofo " + id + " liberou o palito esquerdo."); 
                        palitoDireito.unlock(); 
                        System.out.println("Filósofo " + id + " liberou o palito direito."); 
                    }
                } else {
                    // Threads com identificação ÍMPAR requisitam primeiro o palito esquerdo.
                    palitoEsquerdo.lock(); 
                    System.out.println("Filósofo " + id + " pegou o palito esquerdo."); 
                    
                    palitoDireito.lock(); 
                    System.out.println("Filósofo " + id + " pegou o palito direito."); 

                    try {
                        comer(); 
                    } finally {
                        // Desfaz as travas na ordem inversa garantindo a integridade dos estados adjacentes
                        palitoDireito.unlock(); 
                        System.out.println("Filósofo " + id + " liberou o palito direito."); 
                        palitoEsquerdo.unlock(); 
                        System.out.println("Filósofo " + id + " liberou o palito esquerdo."); 
                    }
                }
            }
        } catch (InterruptedException e) {
            // Tratamento contra interrupções abruptas da thread (Asynchronous Interruption)
            Thread.currentThread().interrupt(); 
        }
    }
}