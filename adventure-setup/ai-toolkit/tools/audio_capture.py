"""Linux Pulse/PipeWire loopback, scoped to a private sink owned by this run.

Never records the microphone or default desktop monitor; never changes defaults.
The game child alone receives PULSE_SINK/ALSOFT_DRIVERS. Close unloads our sink.
"""
from contextlib import contextmanager
import ctypes as C
import ctypes.util
import time
import uuid


@contextmanager
def isolated_sink():
    lib = C.CDLL(ctypes.util.find_library('pulse') or 'libpulse.so.0')
    ptr, integer, string = C.c_void_p, C.c_int, C.c_char_p
    def api(name, result, args):
        fn = getattr(lib, name)
        fn.restype, fn.argtypes = result, args
        return fn
    loop = api('pa_mainloop_new', ptr, [])()
    if not loop:
        raise ValueError('Cannot create Pulse/PipeWire event loop.')
    context = None
    module = None
    operation = None
    original_error = None
    iterate = api('pa_mainloop_iterate', integer, [ptr, integer, ptr])
    state = api('pa_context_get_state', integer, [ptr])
    unref = api('pa_operation_unref', None, [ptr])
    def wait(predicate):
        deadline = time.monotonic()+5
        while not predicate():
            if time.monotonic() > deadline or (context and state(context) in (5,6)):
                raise ValueError('Private audio sink unavailable; Pulse/PipeWire connection failed.')
            iterate(loop,0,None)
            time.sleep(.01)
    try:
        context = api('pa_context_new',ptr,[ptr,string])(
            api('pa_mainloop_get_api',ptr,[ptr])(loop),b'Adventure audio verification')
        if not context:
            raise ValueError('Cannot create Pulse/PipeWire client.')
        if api('pa_context_connect',integer,[ptr,string,integer,ptr])(context,None,0,None) < 0:
            raise ValueError('Cannot connect to Pulse/PipeWire for private audio capture.')
        wait(lambda:state(context)==4)
        sink = 'adventure_test_'+uuid.uuid4().hex
        loaded = []
        callback_type = C.CFUNCTYPE(None,ptr,C.c_uint32,ptr)
        callback = callback_type(lambda ctx,index,user:loaded.append(index))
        operation = api('pa_context_load_module',ptr,[ptr,string,string,callback_type,ptr])(
            context,b'module-null-sink',('sink_name='+sink+' rate=48000 channels=2').encode(),callback,None)
        if not operation:
            raise ValueError('Cannot request isolated audio sink.')
        wait(lambda:bool(loaded))
        module = loaded[0]
        if module == 0xffffffff:
            module = None
            raise ValueError('Pulse/PipeWire rejected the private null sink.')
        unref(operation);operation=None
        yield sink
    except BaseException as exc:
        original_error = exc
        raise
    finally:
        try:
            if operation:
                api('pa_operation_cancel',None,[ptr])(operation)
                unref(operation)
            if module is not None:
                done = []
                finish_type = C.CFUNCTYPE(None,ptr,integer,ptr)
                finish = finish_type(lambda ctx,success,user:done.append(success))
                op = api('pa_context_unload_module',ptr,[ptr,C.c_uint32,finish_type,ptr])(context,module,finish,None)
                if not op:
                    raise ValueError('Private audio sink cleanup could not be requested.')
                try:
                    wait(lambda:bool(done))
                    if not done[0]:raise ValueError('Private audio sink cleanup failed.')
                finally:
                    api('pa_operation_cancel',None,[ptr])(op)
                    unref(op)
        except Exception as cleanup_error:
            if original_error is None:raise
            # Preserve the original failure and include cleanup context on Python 3.10+.
            original_error.args = (*original_error.args, 'Audio cleanup: '+str(cleanup_error))
        finally:
            if context:
                api('pa_context_disconnect',None,[ptr])(context)
                api('pa_context_unref',None,[ptr])(context)
            api('pa_mainloop_free',None,[ptr])(loop)
