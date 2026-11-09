// Validação de confirmação de senha dos formulários web de cadastro.
// O backend repete a conferência para impedir requisições manipuladas.
document.querySelectorAll('form[data-validar-confirmacao-senha]').forEach((formulario) => {
    const senha = formulario.querySelector('#senha');
    const confirmacao = formulario.querySelector('#confirmarSenha');

    // O formulário administrativo também é usado para editar usuários (sem senha).
    if (!senha || !confirmacao) return;

    const mensagem = formulario.querySelector('[data-erro-confirmacao]');
    const mensagemServidor = formulario.querySelector('[data-confirmacao-servidor]');

    const validar = () => {
        const diferente = confirmacao.value.length > 0 && confirmacao.value !== senha.value;
        confirmacao.setCustomValidity(diferente ? 'As senhas não coincidem.' : '');
        confirmacao.setAttribute('aria-invalid', String(diferente));
        if (mensagem) mensagem.hidden = !diferente;
        if (mensagemServidor) mensagemServidor.hidden = true;
    };

    senha.addEventListener('input', validar);
    confirmacao.addEventListener('input', validar);
});
