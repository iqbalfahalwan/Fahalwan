#include <jni.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>
#include <errno.h>
#include <signal.h>
#include <fcntl.h>
#include <sys/select.h>
#include <time.h>
#include <android/log.h>

#define LOG_TAG "Terminal"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define BUFFER_SIZE 4096
#define MAX_OUTPUT_SIZE (1024 * 1024) // 1MB max output
#define TIMEOUT_SEC 60

// Global variables for process management
static pid_t current_pid = -1;

JNIEXPORT jstring JNICALL
Java_iqbal_fahalwan_Terminal_Proses(JNIEnv *env, jobject thiz, jstring baca) {
    const char *cmd = NULL;
    char *output = NULL;
    int stdout_pipe[2] = {-1, -1};
    int stderr_pipe[2] = {-1, -1};
    pid_t pid = -1;
    size_t total_size = 0;
    size_t capacity = BUFFER_SIZE * 4;

    // Check input string
    if (baca == NULL) {
        LOGE("Input command is NULL");
        return (*env)->NewStringUTF(env, "Error: Command tidak boleh kosong");
    }

    // Get command string
    cmd = (*env)->GetStringUTFChars(env, baca, 0);
    if (cmd == NULL) {
        LOGE("Failed to get UTF chars from command");
        return (*env)->NewStringUTF(env, "Error: Gagal membaca command");
    }

    // Validate command
    size_t cmd_len = strlen(cmd);
    if (cmd_len == 0) {
        (*env)->ReleaseStringUTFChars(env, baca, cmd);
        return (*env)->NewStringUTF(env, "Error: Command kosong");
    }
    
    if (cmd_len > 4048) {
        (*env)->ReleaseStringUTFChars(env, baca, cmd);
        return (*env)->NewStringUTF(env, "Error: Command terlalu panjang");
    }

    // Check if proot environment is available
    const char *proot = "/data/data/iqbal.fahalwan/files/logika/bin/proot";
    const char *bash = "/data/data/iqbal.fahalwan/files/logika/ubuntu/bin/bash";
    
    if (access(proot, X_OK) != 0) {
        LOGE("proot not found or not executable: %s", proot);
        (*env)->ReleaseStringUTFChars(env, baca, cmd);
        return (*env)->NewStringUTF(env, "Error: proot tidak ditemukan");
    }

    if (access(bash, X_OK) != 0) {
        LOGE("bash not found or not executable: %s", bash);
        (*env)->ReleaseStringUTFChars(env, baca, cmd);
        return (*env)->NewStringUTF(env, "Error: bash tidak ditemukan");
    }

    // Create pipes for stdout and stderr
    if (pipe(stdout_pipe) == -1) {
        LOGE("Failed to create stdout pipe: %s", strerror(errno));
        (*env)->ReleaseStringUTFChars(env, baca, cmd);
        return (*env)->NewStringUTF(env, "Error: Gagal membuat pipe stdout");
    }
    
    if (pipe(stderr_pipe) == -1) {
        LOGE("Failed to create stderr pipe: %s", strerror(errno));
        close(stdout_pipe[0]);
        close(stdout_pipe[1]);
        (*env)->ReleaseStringUTFChars(env, baca, cmd);
        return (*env)->NewStringUTF(env, "Error: Gagal membuat pipe stderr");
    }

    // Set non-blocking mode on read ends
    fcntl(stdout_pipe[0], F_SETFL, O_NONBLOCK);
    fcntl(stderr_pipe[0], F_SETFL, O_NONBLOCK);

    // Fork process
    pid = fork();
    if (pid == -1) {
        LOGE("Fork failed: %s", strerror(errno));
        close(stdout_pipe[0]); close(stdout_pipe[1]);
        close(stderr_pipe[0]); close(stderr_pipe[1]);
        (*env)->ReleaseStringUTFChars(env, baca, cmd);
        return (*env)->NewStringUTF(env, "Error: Fork gagal");
    }

    if (pid == 0) { // Child process
        // Close read ends
        close(stdout_pipe[0]); 
        close(stderr_pipe[0]);
        
        // Redirect stdout and stderr to pipes
        if (dup2(stdout_pipe[1], STDOUT_FILENO) == -1) {
            perror("dup2 stdout failed");
            _exit(127);
        }
        if (dup2(stderr_pipe[1], STDERR_FILENO) == -1) {
            perror("dup2 stderr failed");
            _exit(127);
        }
        
        // Close write ends
        close(stdout_pipe[1]);
        close(stderr_pipe[1]);

        // Set environment variables
        setenv("PROOT_NO_SECCOMP", "1", 1);
        setenv("PROOT_TMP_DIR", "/data/data/iqbal.fahalwan/files/tmp", 1);
        setenv("HOME", "/root", 1);
        setenv("USER", "root", 1);
        setenv("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin", 1);
        
        // Prepare proot arguments
        const char *root_dir = "/data/data/iqbal.fahalwan/files/logika/ubuntu";
        char *argv[] = {
            (char *)proot,
            "-r", (char *)root_dir, 
            "-b", "/dev",          
            "-b", "/proc",          
            "-b", "/sys",           
            "-b", "/sdcard",           
            "-w", "/root",          
            "--",                 
            (char *)bash,
            "-c", (char *)cmd,
            NULL
        };

        // Execute command
        execv(proot, argv);
        
        // If we get here, execv failed
        perror("execv failed");
        _exit(127);
    }

    // Parent process
    current_pid = pid;
    
    // Close write ends
    close(stdout_pipe[1]);  
    close(stderr_pipe[1]);

    // Allocate output buffer
    output = malloc(capacity);
    if (!output) {
        LOGE("Failed to allocate output buffer");
        close(stdout_pipe[0]);
        close(stderr_pipe[0]);
        kill(pid, SIGTERM);
        waitpid(pid, NULL, 0);
        (*env)->ReleaseStringUTFChars(env, baca, cmd);
        current_pid = -1;
        return (*env)->NewStringUTF(env, "Error: Memory allocation failed");
    }
    output[0] = '\0';

    // Read from pipes
    char buffer[BUFFER_SIZE];
    fd_set read_fds;
    struct timeval timeout;
    int max_fd = (stdout_pipe[0] > stderr_pipe[0]) ? stdout_pipe[0] : stderr_pipe[0];
    
    time_t start_time = time(NULL);
    int process_running = 1;
    
    while (process_running) {
        // Check timeout
        if (time(NULL) - start_time > TIMEOUT_SEC) {
            LOGE("Command timeout after %d seconds", TIMEOUT_SEC);
            kill(pid, SIGKILL);
            break;
        }
        
        // Set up file descriptors for select
        FD_ZERO(&read_fds);
        FD_SET(stdout_pipe[0], &read_fds);
        FD_SET(stderr_pipe[0], &read_fds);
        
        timeout.tv_sec = 1;
        timeout.tv_usec = 0;
        
        int activity = select(max_fd + 1, &read_fds, NULL, NULL, &timeout);
        
        if (activity < 0) {
            if (errno == EINTR) continue;
            LOGE("Select failed: %s", strerror(errno));
            break;
        }
        
        if (activity == 0) {
            // Timeout, check if process is still running
            if (waitpid(pid, NULL, WNOHANG) != 0) {
                process_running = 0;
            }
            continue;
        }
        
        // Read from stdout
        if (FD_ISSET(stdout_pipe[0], &read_fds)) {
            ssize_t bytes_read = read(stdout_pipe[0], buffer, sizeof(buffer) - 1);
            if (bytes_read > 0) {
                buffer[bytes_read] = '\0';
                
                // Check if we need to resize output buffer
                if (total_size + bytes_read + 1 > capacity) {
                    if (capacity >= MAX_OUTPUT_SIZE) {
                        const char *truncated_msg = "\n[Output terpotong - terlalu besar]\n";
                        size_t msg_len = strlen(truncated_msg);
                        if (total_size + msg_len + 1 <= capacity) {
                            strncat(output, truncated_msg, capacity - total_size - 1);
                            total_size += msg_len;
                        }
                        break;
                    }
                    
                    size_t new_capacity = capacity * 2;
                    if (new_capacity > MAX_OUTPUT_SIZE) new_capacity = MAX_OUTPUT_SIZE;
                    
                    char *new_output = realloc(output, new_capacity);
                    if (!new_output) {
                        LOGE("Failed to realloc output buffer");
                        break;
                    }
                    output = new_output;
                    capacity = new_capacity;
                }
                
                // Append to output
                strncat(output, buffer, capacity - total_size - 1);
                total_size += bytes_read;
            } else if (bytes_read == 0) {
                // EOF
                close(stdout_pipe[0]);
                stdout_pipe[0] = -1;
            }
        }
        
        // Read from stderr
        if (FD_ISSET(stderr_pipe[0], &read_fds)) {
            ssize_t bytes_read = read(stderr_pipe[0], buffer, sizeof(buffer) - 1);
            if (bytes_read > 0) {
                buffer[bytes_read] = '\0';
                
                const char *stderr_prefix = "[STDERR] ";
                size_t prefix_len = strlen(stderr_prefix);
                
                // Check if we need to resize output buffer
                if (total_size + bytes_read + prefix_len + 1 > capacity) {
                    if (capacity >= MAX_OUTPUT_SIZE) {
                        const char *truncated_msg = "\n[Output terpotong - terlalu besar]\n";
                        size_t msg_len = strlen(truncated_msg);
                        if (total_size + msg_len + 1 <= capacity) {
                            strncat(output, truncated_msg, capacity - total_size - 1);
                            total_size += msg_len;
                        }
                        break;
                    }
                    
                    size_t new_capacity = capacity * 2;
                    if (new_capacity > MAX_OUTPUT_SIZE) new_capacity = MAX_OUTPUT_SIZE;
                    
                    char *new_output = realloc(output, new_capacity);
                    if (!new_output) {
                        LOGE("Failed to realloc output buffer");
                        break;
                    }
                    output = new_output;
                    capacity = new_capacity;
                }
                
                // Append stderr prefix and content
                strncat(output, stderr_prefix, capacity - total_size - 1);
                total_size += prefix_len;
                strncat(output, buffer, capacity - total_size - 1);
                total_size += bytes_read;
            } else if (bytes_read == 0) {
                // EOF
                close(stderr_pipe[0]);
                stderr_pipe[0] = -1;
            }
        }
        
        // Check if process is still running
        if (waitpid(pid, NULL, WNOHANG) != 0) {
            process_running = 0;
        }
    }

    // Clean up pipes
    if (stdout_pipe[0] != -1) close(stdout_pipe[0]);
    if (stderr_pipe[0] != -1) close(stderr_pipe[0]);

    // Wait for process to complete
    int status;
    waitpid(pid, &status, 0);
    
    // Add exit information to output
    if (WIFEXITED(status)) {
        int exit_code = WEXITSTATUS(status);
        if (exit_code != 0) {
            LOGI("Command exited with code: %d", exit_code);
            char exit_msg[64];
            snprintf(exit_msg, sizeof(exit_msg), "\n[Exit code: %d]", exit_code);
            
            if (total_size + strlen(exit_msg) + 1 <= capacity) {
                strncat(output, exit_msg, capacity - total_size - 1);
            }
        }
    } else if (WIFSIGNALED(status)) {
        int signal_num = WTERMSIG(status);
        LOGI("Command terminated by signal: %d", signal_num);
        
        char signal_msg[64];
        snprintf(signal_msg, sizeof(signal_msg), "\n[Terminated by signal: %d]", signal_num);
        
        if (total_size + strlen(signal_msg) + 1 <= capacity) {
            strncat(output, signal_msg, capacity - total_size - 1);
        }
    }

    // Clean up
    (*env)->ReleaseStringUTFChars(env, baca, cmd);
    current_pid = -1;

    // Return result
    jstring result;
    if (total_size > 0) {
        result = (*env)->NewStringUTF(env, output);
    } else {
        result = (*env)->NewStringUTF(env, "[No output]");
    }
    
    free(output);
    return result;
}

JNIEXPORT void JNICALL
Java_iqbal_fahalwan_Terminal_InterruptCommand(JNIEnv *env, jobject thiz) {
    LOGI("Interrupt command requested, current PID: %d", current_pid);
    
    if (current_pid > 0) {
        if (kill(current_pid, SIGINT) == 0) {
            LOGI("Sent SIGINT to process %d", current_pid);
        } else {
            LOGE("Failed to send signal to process %d: %s", current_pid, strerror(errno));
        }
    } else {
        LOGI("No process running to interrupt");
    }
}

JNIEXPORT jboolean JNICALL
Java_iqbal_fahalwan_Terminal_IsEnvironmentReady(JNIEnv *env, jobject thiz) {
    const char *proot = "/data/data/iqbal.fahalwan/files/logika/bin/proot";
    const char *root_dir = "/data/data/iqbal.fahalwan/files/logika/ubuntu";
    const char *bash = "/data/data/iqbal.fahalwan/files/logika/ubuntu/bin/bash";
    
    if (access(proot, X_OK) != 0) {
        LOGE("proot not executable");
        return JNI_FALSE;
    }
    
    if (access(root_dir, R_OK) != 0) {
        LOGE("ubuntu root directory not accessible");
        return JNI_FALSE;
    }
    
    if (access(bash, X_OK) != 0) {
        LOGE("bash not executable");
        return JNI_FALSE;
    }
    
    return JNI_TRUE;
}