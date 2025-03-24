import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Нужно с помощью Executor Framework
 * обработать очередь транзакций и вывести итоговые данные о счетах каждого пользователя
 */
public class TransactionQueue {
    /**
     * Вспомогательный класс для представления транзакции.
     */
    static class Transaction {
        final int fromId;
        final int toId;
        final int amount;

        Transaction(int fromId, int toId, int amount) {
            this.fromId = fromId;
            this.toId = toId;
            this.amount = amount;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Введите количество пользователей:");
        int n = scanner.nextInt();
        int[] balances = new int[n];
        ReentrantLock[] locks = new ReentrantLock[n];

        System.out.println("Введите начальные балансы пользователей:");
        for (int i = 0; i < n; i++) {
            balances[i] = scanner.nextInt();
            locks[i] = new ReentrantLock(); // Создаем блокировку для каждого пользователя
        }

        System.out.println("Введите количество транзакций:");
        int m = scanner.nextInt();
        scanner.nextLine(); // Переход на следующую строку

        Transaction[] transactions = new Transaction[m];
        System.out.println("Введите транзакции в формате: fromId - amount - toId");
        for (int i = 0; i < m; i++) {
            String[] parts = scanner.nextLine().split(" - ");
            int fromId = Integer.parseInt(parts[0]);
            int amount = Integer.parseInt(parts[1]);
            int toId = Integer.parseInt(parts[2]);
            transactions[i] = new Transaction(fromId, toId, amount);
        }
        scanner.close();

        // Создаем пул потоков
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(m, 4));

        for (Transaction transaction : transactions) {
            executor.execute(() -> processTransaction(transaction, balances, locks));
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            // Ждем завершения всех задач
        }

        // Вывод финального баланса
        for (int i = 0; i < n; i++) {
            System.out.println("User " + i + " final balance: " + balances[i]);
        }
    }

    /**
     * Обрабатывает транзакцию, гарантируя потокобезопасное изменение балансов.
     */
    private static void processTransaction(Transaction transaction, int[] balances, ReentrantLock[] locks) {
        int fromId = transaction.fromId;
        int toId = transaction.toId;
        int amount = transaction.amount;

        if (fromId == toId) return; // Исключаем транзакции самому себе

        // Берем блокировки в одном порядке, чтобы избежать deadlock
        ReentrantLock firstLock = fromId < toId ? locks[fromId] : locks[toId];
        ReentrantLock secondLock = fromId < toId ? locks[toId] : locks[fromId];

        firstLock.lock();
        secondLock.lock();
        try {
            if (balances[fromId] >= amount) {
                balances[fromId] -= amount;
                balances[toId] += amount;
            }
        } finally {
            secondLock.unlock();
            firstLock.unlock();
        }
    }
}
