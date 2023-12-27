public class Main {
    public static void main(String[] args) {
        int[] number = new int[args.length + 1];

        for (int i = 0; i < args.length; i++) {
            number[i] = Integer.parseInt(args[i]);
        }
        System.out.println(jump(number));
    }
    public static int jump(int[] nums) {
        int n = nums.length;
        // 众神归位
        for (int i = 0; i < n; i++) {
            while (nums[i] > 0 && nums[i] <= n) {
                int pos = nums[i] - 1;
                // 交换 i 和 pos
                if (nums[i] == nums[pos]) break; // 已经在对应位置
                int t = nums[i];
                nums[i] = nums[pos];
                nums[pos] = t;
            }
        }
        // 找第一个
        for (int i = 0; i < n; i++) {
            if (nums[i] != i + 1) return i + 1;
        }
        return n + 1;
    }

}