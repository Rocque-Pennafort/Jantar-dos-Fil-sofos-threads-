import java.util.concurrent.locks.Lock; 
import java.util.concurrent.locks.ReentrantLock; 
import java.util.concurrent.atomic.AtomicBoolean; 

public class Jantar { 
    public static void main(String[] args) {
        int numFilosofos = 5; // Define o número de threads operárias concorrentes
        long tempoExecucao = 10000; // Tempo delimitado para a execução global (10s)
        
        // Recursos compartilhados (Regiões Críticas representadas por travas de exclusão mútua)
        Lock[] palitos = new ReentrantLock[numFilosofos]; 
        Thread[] filosofos = new Thread[numFilosofos]; 
        
        // Sinalizador atômico volátil, garantindo visibilidade thread-safe sem travar a CPU
        AtomicBoolean continuar = new AtomicBoolean(true); 

        // Criação das travas de exclusão mútua (Locks) para cada recurso compartilhado
        for (int i = 0; i < numFilosofos; i++) {
            palitos[i] = new ReentrantLock(); 
        }

        // Instanciação e Inicialização das Threads dos Filósofos
        for (int i = 0; i < numFilosofos; i++) {
            // Mapeamento dos recursos compartilhados adjacentes a cada thread operária
            filosofos[i] = new Thread(new Filosofo(i, palitos[i], palitos[(i + 1) % numFilosofos], continuar));
            
            // Transição de estado da Thread: NEW -> RUNNABLE (Pronta para ser escalonada pela JVM)
            filosofos[i].start(); 
        }

        // Inicialização de uma Thread dedicada ao controle temporal do sistema (Timer)
        Timer timer = new Timer(tempoExecucao, continuar, filosofos);
        timer.start(); 
    }
}

// Thread Gerenciadora: Responsável por gerenciar o ciclo de vida final das demais threads operárias
class Timer extends Thread {
    private final long duracao; 
    private final AtomicBoolean continuar; 
    private final Thread[] filosofos; 

    public Timer(long duracao, AtomicBoolean continuar, Thread[] filosofos) {
        this.duracao = duracao; 
        this.continuar = continuar; 
        this.filosofos = filosofos; 
    }

    @Override
    public void run() {
        try {
            System.out.println("Timer iniciado: a execução vai durar " + duracao + "ms"); 
            // Coloca a thread Timer em estado TIMED_WAITING, liberando o processador
            Thread.sleep(duracao); 
            
            System.out.println("\n=== TEMPO FINALIZADO ==="); 
            // Modifica a flag atômica para alertar todas as threads concorrentes a saírem do loop
            continuar.set(false); 
            
            // BARREIRA DE SINCRONIZAÇÃO: Aguarda a conclusão limpa de cada thread operária
            for (Thread filosofo : filosofos) {
                // A thread Timer entra em estado WAITING até que a thread 'filosofo' correspondente finalize
                filosofo.join(); 
            }
            
            System.out.println("Todos os filósofos pararam de comer."); 
        } catch (InterruptedException e) {
            // Restaura o status de interrupção da thread seguindo as boas práticas de concorrência
            Thread.currentThread().interrupt(); 
        }
    }
}