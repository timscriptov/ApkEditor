#include <unistd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/ptrace.h>
#include <sys/prctl.h>
#include <sys/wait.h>
#include <sys/user.h>
#include <dirent.h>
#include <string.h>
#include <signal.h>
#include <errno.h>
#include <stdio.h>
#ifndef   SINGLESTEPS
#define   SINGLESTEPS 10
#endif

/* Similar to getline(), except gets process pid task IDs.
 * Returns positive (number of TIDs in list) if success,
 * otherwise 0 with errno set. */
size_t get_tids(pid_t **const listptr, size_t *const sizeptr, const pid_t pid)
{
    char     dirname[64];
    DIR     *dir;
    pid_t   *list;
    size_t   size, used = 0;

    if (!listptr || !sizeptr || pid < (pid_t)1) {
        errno = EINVAL;
        return (size_t)0;
    }

    if (*sizeptr > 0) {
        list = *listptr;
        size = *sizeptr;
    } else {
        list = *listptr = NULL;
        size = *sizeptr = 0;
    }

    if (snprintf(dirname, sizeof dirname, "/proc/%d/task/", (int)pid) >= (int)sizeof dirname) {
        errno = ENOTSUP;
        return (size_t)0;
    }

    dir = opendir(dirname);
    if (!dir) {
        errno = ESRCH;
        return (size_t)0;
    }

    while (1) {
        struct dirent *ent;
        int            value;
        char           dummy;

        errno = 0;
        ent = readdir(dir);
        if (!ent)
            break;

        /* Parse TIDs. Ignore non-numeric entries. */
        if (sscanf(ent->d_name, "%d%c", &value, &dummy) != 1)
            continue;

        /* Ignore obviously invalid entries. */
        if (value < 1)
            continue;

        /* Make sure there is room for another TID. */
        if (used >= size) {
            size = (used | 127) + 128;
            list = realloc(list, size * sizeof list[0]);
            if (!list) {
                closedir(dir);
                errno = ENOMEM;
                return (size_t)0;
            }
            *listptr = list;
            *sizeptr = size;
        }

        /* Add to list. */
        list[used++] = (pid_t)value;
    }
    if (errno) {
        const int saved_errno = errno;
        closedir(dir);
        errno = saved_errno;
        return (size_t)0;
    }
    if (closedir(dir)) {
        errno = EIO;
        return (size_t)0;
    }

    /* None? */
    if (used < 1) {
        errno = ESRCH;
        return (size_t)0;
    }

    /* Make sure there is room for a terminating (pid_t)0. */
    if (used >= size) {
        size = used + 1;
        list = realloc(list, size * sizeof list[0]);
        if (!list) {
            errno = ENOMEM;
            return (size_t)0;
        }
        *listptr = list;
        *sizeptr = size;
    }

    /* Terminate list; done. */
    list[used] = (pid_t)0;
    errno = 0;
    return used;
}


static int wait_process(const pid_t pid, int *const statusptr)
{
    int   status;
    pid_t p;

    do {
        status = 0;
        p = waitpid(pid, &status, WUNTRACED | WCONTINUED);
    } while (p == (pid_t)-1 && errno == EINTR);
    if (p != pid)
        return errno = ESRCH;

    if (statusptr)
        *statusptr = status;

    return errno = 0;
}

static int continue_process(const pid_t pid, int *const statusptr)
{
    int   status;
    pid_t p;

    do {

        if (kill(pid, SIGCONT) == -1)
            return errno = ESRCH;

        do {
            status = 0;
            p = waitpid(pid, &status, WUNTRACED | WCONTINUED);
        } while (p == (pid_t)-1 && errno == EINTR);

        if (p != pid)
            return errno = ESRCH;

    } while (WIFSTOPPED(status));

    if (statusptr)
        *statusptr = status;

    return errno = 0;
}

void show_registers(FILE *const out, pid_t tid, const char *const note)
{
    struct user_regs_struct regs;
    long                    r;

    do {
        r = ptrace(PTRACE_GETREGS, tid, &regs, &regs);
    } while (r == -1L && errno == ESRCH);
    if (r == -1L)
        return;

#if (defined(__x86_64__) || defined(__i386__)) && __WORDSIZE == 64
    if (note && *note)
        fprintf(out, "Task %d: RIP=0x%016lx, RSP=0x%016lx. %s\n", (int)tid, regs.rip, regs.rsp, note);
    else
        fprintf(out, "Task %d: RIP=0x%016lx, RSP=0x%016lx.\n", (int)tid, regs.rip, regs.rsp);
#elif (defined(__x86_64__) || defined(__i386__)) && __WORDSIZE == 32
    if (note && *note)
        fprintf(out, "Task %d: EIP=0x%08xx, ESP=0x%08x. %s\n", (int)tid, regs.eip, regs.rsp, note);
    else
        fprintf(out, "Task %d: EIP=0x%08xx, ESP=0x%08x.\n", (int)tid, regs.eip, regs.rsp);
#endif
}


int main(int argc, char *argv[])
{
    pid_t *tid = 0;
    size_t tids = 0;
    size_t tids_max = 0;
    size_t t, s;
    long   r;

    pid_t child;
    int   status;

    if (argc < 2 || !strcmp(argv[1], "-h") || !strcmp(argv[1], "--help")) {
        fprintf(stderr, "\n");
        fprintf(stderr, "Usage: %s [ -h | --help ]\n", argv[0]);
        fprintf(stderr, "       %s COMMAND [ ARGS ... ]\n", argv[0]);
        fprintf(stderr, "\n");
        fprintf(stderr, "This program executes COMMAND in a child process,\n");
        fprintf(stderr, "and waits for it to stop (via a SIGSTOP signal).\n");
        fprintf(stderr, "When that occurs, the register state of each thread\n");
        fprintf(stderr, "is dumped to standard output, then the child process\n");
        fprintf(stderr, "is sent a SIGCONT signal.\n");
        fprintf(stderr, "\n");
        return 1;
    }

    child = fork();
    if (child == (pid_t)-1) {
        fprintf(stderr, "fork() failed: %s.\n", strerror(errno));
        return 1;
    }

    if (!child) {
        prctl(PR_SET_DUMPABLE, (long)1);
        prctl(PR_SET_PTRACER, (long)getppid());
        fflush(stdout);
        fflush(stderr);
        execvp(argv[1], argv + 1);
        fprintf(stderr, "%s: %s.\n", argv[1], strerror(errno));
        return 127;
    }

    fprintf(stderr, "Tracer: Waiting for child (pid %d) events.\n\n", (int)child);
    fflush(stderr);

    while (1) {

        /* Wait for a child event. */
        if (wait_process(child, &status))
            break;

        /* Exited? */
        if (WIFEXITED(status) || WIFSIGNALED(status)) {
            errno = 0;
            break;
        }

        /* At this point, only stopped events are interesting. */
        if (!WIFSTOPPED(status))
            continue;

        /* Obtain task IDs. */
        tids = get_tids(&tid, &tids_max, child);
        if (!tids)
            break;

        printf("Process %d has %d tasks,", (int)child, (int)tids);
        fflush(stdout);

        /* Attach to all tasks. */
        for (t = 0; t < tids; t++) {
            do {
                r = ptrace(PTRACE_ATTACH, tid[t], (void *)0, (void *)0);
            } while (r == -1L && (errno == EBUSY || errno == EFAULT || errno == ESRCH));
            if (r == -1L) {
                const int saved_errno = errno;
                while (t-->0)
                    do {
                        r = ptrace(PTRACE_DETACH, tid[t], (void *)0, (void *)0);
                    } while (r == -1L && (errno == EBUSY || errno == EFAULT || errno == ESRCH));
                tids = 0;
                errno = saved_errno;
                break;
            }
        }
        if (!tids) {
            const int saved_errno = errno;
            if (continue_process(child, &status))
                break;
            printf(" failed to attach (%s).\n", strerror(saved_errno));
            fflush(stdout);
            if (WIFCONTINUED(status))
                continue;
            errno = 0;
            break;
        }

        printf(" attached to all.\n\n");
        fflush(stdout);

        /* Dump the registers of each task. */
        for (t = 0; t < tids; t++)
            show_registers(stdout, tid[t], "");
        printf("\n");
        fflush(stdout);

        for (s = 0; s < SINGLESTEPS; s++) {
            do {
                r = ptrace(PTRACE_SINGLESTEP, tid[tids-1], (void *)0, (void *)0);
            } while (r == -1L && errno == ESRCH);
            if (!r) {
                for (t = 0; t < tids - 1; t++)
                    show_registers(stdout, tid[t], "");
                show_registers(stdout, tid[tids-1], "Advanced by one step.");
                printf("\n");
                fflush(stdout);
            } else {
                fprintf(stderr, "Single-step failed: %s.\n", strerror(errno));
                fflush(stderr);
            }
        }

        /* Detach from all tasks. */
        for (t = 0; t < tids; t++)
            do {
                r = ptrace(PTRACE_DETACH, tid[t], (void *)0, (void *)0);
            } while (r == -1 && (errno == EBUSY || errno == EFAULT || errno == ESRCH));
        tids = 0;
        if (continue_process(child, &status))
            break;
        if (WIFCONTINUED(status)) {
            printf("Detached. Waiting for new stop events.\n\n");
            fflush(stdout);
            continue;
        }
        errno = 0;
        break;
    }
    if (errno)
        fprintf(stderr, "Tracer: Child lost (%s)\n", strerror(errno));
    else
    if (WIFEXITED(status))
        fprintf(stderr, "Tracer: Child exited (%d)\n", WEXITSTATUS(status));
    else
    if (WIFSIGNALED(status))
        fprintf(stderr, "Tracer: Child died from signal %d\n", WTERMSIG(status));
    else
        fprintf(stderr, "Tracer: Child vanished\n");
    fflush(stderr);

    return status;
}
